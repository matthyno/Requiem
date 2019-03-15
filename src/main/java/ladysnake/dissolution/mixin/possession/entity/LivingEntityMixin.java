package ladysnake.dissolution.mixin.possession.entity;

import ladysnake.dissolution.api.v1.DissolutionPlayer;
import ladysnake.dissolution.api.v1.possession.Possessable;
import ladysnake.dissolution.common.entity.ai.attribute.AttributeHelper;
import ladysnake.dissolution.common.entity.ai.attribute.CooldownStrengthAttribute;
import ladysnake.dissolution.mixin.entity.LivingEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AbstractEntityAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.TranslatableTextComponent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Possessable {
    @Nullable
    @Shadow
    public abstract EntityAttributeInstance getAttributeInstance(EntityAttribute entityAttribute_1);

    @Shadow public abstract AbstractEntityAttributeContainer getAttributeContainer();

    @Shadow public abstract float getAbsorptionAmount();

    @Shadow public float headYaw;
    @Shadow public float field_6283;
    @Shadow public float field_6249;
    @Shadow public float field_6225;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    /* * * * * * * * * * *
        Entity overrides
    * * * * * * * * * * */

    @Inject(method = "initAttributes", at = @At("TAIL"))
    private void initAttributes(CallbackInfo ci) {
        if (this.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE) != null) {
            AttributeHelper.substituteAttributeInstance(this.getAttributeContainer(), new CooldownStrengthAttribute((LivingEntity & Possessable)(Object)this));
        }
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void update(CallbackInfo ci) {
        PlayerEntity player = this.getPossessorEntity();
        if (player != null) {
            // Make possessed monsters despawn gracefully
            if (!this.world.isClient) {
                if (this instanceof Monster && this.world.getDifficulty() == Difficulty.PEACEFUL) {
                    player.addChatMessage(new TranslatableTextComponent("dissolution.message.peaceful_despawn"), true);
                }
            }
            // Set the player's hit timer for damage animation and stuff
            player.field_6008 = this.field_6008;
            player.setAbsorptionAmount(this.getAbsorptionAmount());
        }
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void travel(Vec3d direction, CallbackInfo ci) {
        PlayerEntity player = this.getPossessorEntity();
        if (player != null) {
            this.setRotation(player.yaw, player.pitch);
            this.headYaw = this.field_6283 = this.prevYaw = this.yaw;
            this.method_5796(player.isSwimming());
            // Prevent this entity from taking fall damage unless triggered by the possessor
            this.fallDistance = 0;

            this.setPosition(player.x, player.y, player.z);
            // update limb movement
            this.field_6249 = player.field_6249;
            this.field_6225 = player.field_6225;
        }
    }

    @Inject(method = "updateLogic", at = @At("TAIL"))
    private void updateLogic(CallbackInfo ci) {
        this.getMobAbilityController().update();
    }

    @SuppressWarnings("InvalidMemberReference")
    @Inject(method = {"pushAwayFrom", "pushAway"}, at = @At("HEAD"), cancellable = true)
    public void pushAwayFrom(Entity entity, CallbackInfo ci) {
        // Prevent infinite propulsion through self collision
        if (entity == this.getPossessorEntity()) {
            ci.cancel();
        }
    }

    @Inject(method = "onDeath", at = @At("TAIL"))
    private void onDeath(DamageSource damageSource_1, CallbackInfo ci) {
        // Drop player inventory on death
        PlayerEntity possessor = this.getPossessorEntity();
        if (possessor != null) {
            ((DissolutionPlayer)possessor).getPossessionComponent().stopPossessing();
            if (!world.isClient && !possessor.isCreative() && !world.getGameRules().getBoolean("keepInventory")) {
                possessor.inventory.dropAll();
            }
        }
    }

    @Inject(method = "scheduleVelocityUpdate", at = @At("RETURN"))
    private void scheduleVelocityUpdate(CallbackInfo ci) {
        PlayerEntity player = this.getPossessorEntity();
        if (!world.isClient && this.velocityModified && player != null) {
            player.velocityModified = true;
        }
    }

    @Inject(method = "method_6076", at = @At("HEAD"), cancellable = true)
    private void updateHeldItem(CallbackInfo ci) {
        if (this.isBeingPossessed()) {
            ci.cancel();
        }
    }

    /* * * * * * * * * * * *
        Delegation land
     * * * * * * * * * * * */
    /**
     * Knockback
     */
    @Inject(method = "method_6005", at = @At("HEAD"), cancellable = true)
    private void knockback(Entity entity, float vx, double vy, double vz, CallbackInfo ci) {
        PlayerEntity possessing = getPossessorEntity();
        if (possessing != null) {
            possessing.method_6005(entity, vx, vy, vz);
            ci.cancel();
        }
    }

    /**
     * Teleport
     * Returns <code>true</code> if the teleportation is successful, otherwise <code>false</code>
     *
     * @param enderTp <code>true</code> for ender particles and sound effect
     */
    @Inject(method = "method_6082", at = @At("HEAD"), cancellable = true)
    private void method_6082(double x, double y, double z, boolean enderTp, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = this.getPossessorEntity();
        if (player != null) {
            cir.setReturnValue(player.method_6082(x, y, z, enderTp));
        }
    }

    @Inject(method = "isFallFlying", at = @At("HEAD"), cancellable = true)
    private void isFallFlying(CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = this.getPossessorEntity();
        if (player != null) {
            cir.setReturnValue(player.isFallFlying());
        }
    }

    /**
     * Returns Whether this entity is using a shield or equivalent
     */
    @Inject(method = "method_6039", at = @At("HEAD"), cancellable = true)
    private void method_6039(CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity possessor = this.getPossessorEntity();
        if (possessor != null) {
            cir.setReturnValue(possessor.method_6039());
        }
    }

    @Inject(method = "damageShield", at = @At("HEAD"), cancellable = true)
    private void damageShield(float damage, CallbackInfo ci) {
        PlayerEntity possessor = this.getPossessorEntity();
        if (possessor != null && !this.world.isClient) {
            ((LivingEntityAccessor)possessor).invokeDamageShield(damage);
            this.world.summonParticle(possessor, (byte)29);
            ci.cancel();
        }
    }

    @Inject(method = "getActiveItem", at = @At("HEAD"), cancellable = true)
    private void getActiveItem(CallbackInfoReturnable<ItemStack> cir) {
        PlayerEntity possessor = this.getPossessorEntity();
        if (possessor != null) {
            cir.setReturnValue(possessor.getActiveItem());
        }
    }

    @Inject(method = "isEquippedStackValid", at = @At("HEAD"), cancellable = true)
    private void isEquippedStackValid(EquipmentSlot slot, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity possessor = this.getPossessorEntity();
        if (possessor != null) {
            cir.setReturnValue(possessor.isEquippedStackValid(slot));
        }
    }

    /**
     * Returns true if this entity's main hand is active
     */
    @Inject(method = "isUsingItem", at = @At("HEAD"), cancellable = true)
    private void isUsingItem(CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity possessor = this.getPossessorEntity();
        if (possessor != null) {
            cir.setReturnValue(possessor.isUsingItem());
        }
    }
}
