package ladysnake.dissolution.common.item;

import ladysnake.dissolution.Dissolution;
import ladysnake.dissolution.api.v1.DissolutionPlayer;
import ladysnake.dissolution.api.v1.remnant.RemnantHandler;
import ladysnake.dissolution.client.ShaderHandler;
import ladysnake.dissolution.common.impl.remnant.DefaultRemnantHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.TranslatableTextComponent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DebugItem extends Item {
    private int debugMode;

    public DebugItem(Settings item$Settings_1) {
        super(item$Settings_1);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (player.isSneaking() && !world.isClient) {
            debugMode = (debugMode + 1) % 2;
            player.addChatMessage(new TranslatableTextComponent("Switched mode to %s", debugMode), true);
        } else {
            switch (debugMode) {
                case 0:
                    RemnantHandler cap = ((DissolutionPlayer)player).getRemnantHandler();
                    Dissolution.LOGGER.info("Player was {}", cap != null && cap.isSoul() ? "incorporeal" : "corporeal");
                    if (!world.isClient) {
                        if (cap == null) {
                            Dissolution.LOGGER.info("Turned {} into a remnant", player);
                            ((DissolutionPlayer)player).setRemnantHandler(cap = new DefaultRemnantHandler(player));
                        }
                        cap.setSoul(!cap.isSoul());
                    }
                    break;
                case 1:
                    break;
            }
        }
        return new TypedActionResult<>(ActionResult.SUCCESS, player.getStackInHand(hand));
    }

    @Override
    public boolean interactWithEntity(ItemStack itemStack_1, PlayerEntity playerEntity_1, LivingEntity livingEntity_1, Hand hand_1) {
        if (debugMode == 1 && !playerEntity_1.world.isClient) {
            ShaderHandler.INSTANCE.beginFishEyeAnimation(livingEntity_1);
        }
        return true;
    }

    @Override
    public boolean beforeBlockBreak(BlockState blockState_1, World world_1, BlockPos blockPos_1, PlayerEntity playerEntity_1) {
        return super.beforeBlockBreak(blockState_1, world_1, blockPos_1, playerEntity_1);
    }
}