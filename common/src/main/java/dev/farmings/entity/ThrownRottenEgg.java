package dev.farmings.entity;

import dev.farmings.FarmingsEntities;
import dev.farmings.FarmingsItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A thrown rotten egg (docs/eggs.md, "Loose ends"): flies like a snowball, hatches nothing, and gives what it hits Nausea for 5 seconds. */
public class ThrownRottenEgg extends ThrowableItemProjectile {
	/** Nausea on a hit, in ticks: 5 seconds *(proposed)*. */
	public static final int NAUSEA_TICKS = 100;

	public ThrownRottenEgg(EntityType<? extends ThrownRottenEgg> type, Level level) {
		super(type, level);
	}

	public ThrownRottenEgg(Level level, LivingEntity owner, ItemStack stack) {
		super(FarmingsEntities.ROTTEN_EGG, owner, level, stack);
	}

	@Override
	protected Item getDefaultItem() {
		return FarmingsItems.ROTTEN_EGG;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 3) {
			ItemParticleOption p = new ItemParticleOption(ParticleTypes.ITEM, getDefaultItem());
			for (int i = 0; i < 8; i++) {
				level().addParticle(p, getX(), getY(), getZ(), (random.nextFloat() - 0.5) * 0.08, (random.nextFloat() - 0.5) * 0.08, (random.nextFloat() - 0.5) * 0.08);
			}
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (hit.getEntity() instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, NAUSEA_TICKS), getOwner());
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (!level().isClientSide()) {
			level().broadcastEntityEvent(this, (byte) 3);
			discard();
		}
	}
}
