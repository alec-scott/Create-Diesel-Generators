package com.jesz.createdieselgenerators.content.tools;

import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.EquipmentSlot;

import com.jesz.createdieselgenerators.CDGConfig;
import com.jesz.createdieselgenerators.CDGDataComponents;
import com.jesz.createdieselgenerators.CreateDieselGenerators;
import com.jesz.createdieselgenerators.world.OilChunksSavedData;
import com.zurrtum.create.AllSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class OilScannerItem extends Item {
    public OilScannerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getY() < CDGConfig.MAX_OIL_SCANNER_LEVEL.get()) {
            stack.set(CDGDataComponents.OIL_SCANNER_PROGRESS, 20);
            stack.set(CDGDataComponents.OIL_SCANNER_STATE, 0);
            if (level.isClientSide())
                player.sendOverlayMessage(CreateDieselGenerators.lang("actionbar.oil_scanner.searching"));

        } else {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), AllSoundEvents.DENY.getMainEvent(), SoundSource.PLAYERS, 1.2f, 1, true);
            if (level.isClientSide())
                player.sendOverlayMessage(CreateDieselGenerators.lang("actionbar.oil_scanner.too_high_up"));
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (!stack.has(CDGDataComponents.OIL_SCANNER_PROGRESS))
            return;
        if (stack.getOrDefault(CDGDataComponents.OIL_SCANNER_STATE, 1) != 0)
            return;

        // PORT-NOTE(26.1): inventoryTick only runs on the server now; the scan tick sound is played from there
        ServerLevel sl = level;
        {
            if (stack.get(CDGDataComponents.OIL_SCANNER_PROGRESS) == 0) {
                stack.set(CDGDataComponents.OIL_SCANNER_PROGRESS, 20);

                ChunkPos chunk = ChunkPos.containing(new BlockPos(entity.getBlockX(), 0, entity.getBlockZ()));

                int amount = OilChunksSavedData.getChunkOilAmount(sl, chunk);

                if (amount <= 0)
                    stack.set(CDGDataComponents.OIL_SCANNER_STATE, 1);
                else if (amount >= (CDGConfig.OIL_CHUNK_THRESHOLD.get() + CDGConfig.OIL_CHUNK_INFINITE_THRESHOLD.get()) / 2)
                    stack.set(CDGDataComponents.OIL_SCANNER_STATE, 3);
                else
                    stack.set(CDGDataComponents.OIL_SCANNER_STATE, 2);
                if (entity instanceof ServerPlayer sp) {
                    if (amount <= 0)
                        sp.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("createdieselgenerators.actionbar.oil_scanner.oil_none", makeProgressBar(3, 0)).withStyle(ChatFormatting.GRAY)));
                    else if (amount == Integer.MAX_VALUE)
                        sp.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("createdieselgenerators.actionbar.oil_scanner.oil_bottomless", makeProgressBar(3, 3)).withStyle(ChatFormatting.GOLD)));
                    else if (amount >= (CDGConfig.OIL_CHUNK_THRESHOLD.get() + CDGConfig.OIL_CHUNK_INFINITE_THRESHOLD.get()) / 2)
                        sp.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("createdieselgenerators.actionbar.oil_scanner.oil_high", makeProgressBar(3, 2)).withStyle(ChatFormatting.YELLOW)));
                    else
                        sp.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("createdieselgenerators.actionbar.oil_scanner.oil_low", makeProgressBar(3, 1)).withStyle(ChatFormatting.GREEN)));

                }

            }
            stack.set(CDGDataComponents.OIL_SCANNER_PROGRESS, stack.getOrDefault(CDGDataComponents.OIL_SCANNER_PROGRESS, 20) - 1);
            if (stack.getOrDefault(CDGDataComponents.OIL_SCANNER_STATE, 1) == 0)
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), AllSoundEvents.SCROLL_VALUE.getMainEvent(), SoundSource.PLAYERS, 0.2f, 1);
        }

        super.inventoryTick(stack, level, entity, slot);
    }

    // PORT-NOTE(26.1): was the client TooltipHelper.makeProgressBar (Create Fly keeps it client side); the item model
    // selects on the oil_scanner_state component (item definition JSON), so the ItemProperties/datagen code is gone.
    private static String makeProgressBar(int length, int filledLength) {
        return " " + "█".repeat(Math.max(0, filledLength)) + "▒".repeat(Math.max(0, length - filledLength)) + " ";
    }
}
