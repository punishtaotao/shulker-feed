package com.shulkerfeed;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

/**
 * 核心补给逻辑（mojmap 风格，对齐本地 26.2 环境）：
 * 当玩家主手持某方块时，若背包里（不含潜影盒内部）该方块不足一组，
 * 依次从【背包内揣着的潜影盒】和【附近地面上的潜影盒（半径 4 格）】
 * 取出该方块补满，使按投影连续放置不断料。
 *
 * 背包内潜影盒内容走 26.2 的 Data Components（DataComponents.CONTAINER -> ItemContainerContents），
 * 不再读取老式 NBT；仅修改后再写回 CONTAINER 组件。
 * 地面潜影盒使用稳定的 Container 接口访问。
 */
public class ShulkerFeeder {
    private static final int RADIUS = 4;          // 附近地面潜影盒搜索半径（格）
    private static final int TARGET_STACK = 64;   // 补足到一组
    private static final int SHULKER_SLOTS = 27;  // 潜影盒 27 槽

    public static void feed(Player player) {
        if (player == null || player.isSpectator()) return;

        ItemStack hand = player.getMainHandItem();
        if (!(hand.getItem() instanceof BlockItem)) return; // 只在「手持方块」时补给

        Item target = hand.getItem();
        Inventory inv = player.getInventory();
        int visible = countVisible(inv, target);
        if (visible >= TARGET_STACK) return;       // 已经有一组，不补
        int need = TARGET_STACK - visible;

        // 1) 背包内揣着的潜影盒
        need = takeFromBackpackShulkers(inv, target, need);

        // 2) 附近地面上的潜影盒
        if (need > 0) {
            Level world = player.level();
            if (world instanceof ServerLevel) {
                BlockPos origin = player.blockPosition();
                outer:
                for (int dx = -RADIUS; dx <= RADIUS; dx++)
                    for (int dy = -RADIUS; dy <= RADIUS; dy++)
                        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                            BlockPos pos = origin.offset(dx, dy, dz);
                            if (world.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity be) {
                                need = takeFromShulker(inv, be, target, need);
                                if (need <= 0) break outer;
                            }
                        }
            }
        }
    }

    /** 遍历背包所有槽，从每个潜影盒物品中取料，直到补满或没有更多来源。 */
    private static int takeFromBackpackShulkers(Inventory inv, Item target, int need) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack slot = inv.getItem(i);
            if (!isShulkerBox(slot)) continue;
            ItemStack copy = slot.copy();           // 在副本上改，避免污染原栈
            int before = need;
            need = takeFromShulkerStack(inv, copy, target, need);
            if (need != before) {
                inv.setItem(i, copy);              // 把改好的潜影盒写回原槽
            }
            if (need <= 0) break;
        }
        return need;
    }

    private static boolean isShulkerBox(ItemStack s) {
        return s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
    }

    /** 从单个「物品形式的潜影盒」取料（直接修改传入 stack 的 CONTAINER 组件）。 */
    private static int takeFromShulkerStack(Inventory inv, ItemStack shulker, Item target, int need) {
        ItemContainerContents contents = shulker.get(DataComponents.CONTAINER);
        if (contents == null) return need;

        NonNullList<ItemStack> items = NonNullList.withSize(SHULKER_SLOTS, ItemStack.EMPTY);
        contents.copyInto(items);

        boolean changed = false;
        for (int i = 0; i < items.size(); i++) {
            ItemStack slot = items.get(i);
            if (slot.getItem() == target && need > 0) {
                int take = Math.min(slot.getCount(), need);
                ItemStack moved = slot.copy();
                moved.setCount(take);
                int remaining = insert(inv, moved);
                int inserted = take - remaining;
                if (inserted > 0) {
                    slot.shrink(inserted);
                    items.set(i, slot);
                    need -= inserted;
                    changed = true;
                }
            }
        }

        if (changed) {
            shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        }
        return need;
    }

    private static int countVisible(Inventory inv, Item target) {
        int c = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() == target) c += s.getCount();
        }
        return c;
    }

    private static int takeFromShulker(Inventory inv, ShulkerBoxBlockEntity be, Item target, int need) {
        int size = be.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack s = be.getItem(i);
            if (s.getItem() == target && need > 0) {
                int take = Math.min(s.getCount(), need);
                ItemStack moved = s.copy();
                moved.setCount(take);
                int remaining = insert(inv, moved);
                int inserted = take - remaining;
                if (inserted > 0) {
                    s.shrink(inserted);
                    be.setItem(i, s);
                    be.setChanged();
                    need -= inserted;
                }
            }
        }
        return need;
    }

    /**
     * 安全插入玩家库存：先合并到同类栈，再填空槽。返回未能插入的剩余数量（0 表示全部插入）。
     */
    private static int insert(Inventory inv, ItemStack toAdd) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.getItem() == toAdd.getItem()
                    && s.getCount() < s.getMaxStackSize()) {
                int space = s.getMaxStackSize() - s.getCount();
                int move = Math.min(space, toAdd.getCount());
                s.grow(move);
                toAdd.shrink(move);
                inv.setItem(i, s);
                if (toAdd.isEmpty()) return 0;
            }
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, toAdd.copy());
                toAdd.setCount(0);
                return 0;
            }
        }
        return toAdd.getCount();
    }
}
