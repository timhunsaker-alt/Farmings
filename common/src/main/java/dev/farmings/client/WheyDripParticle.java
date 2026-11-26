package dev.farmings.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * A drop of whey ({@link dev.farmings.FarmingsParticles#WHEY_DRIP}) on the side of the cheese press's mold: it hangs a moment, then runs
 * down about a third of a block and is gone. Translucent pale yellow-green (whey is the thin, greenish-yellow liquid left from the curd).
 * Drawn with vanilla's drip sprites (the hanging drop, then the falling one), tinted. No collision: it starts inside the press's box.
 */
public class WheyDripParticle extends SingleQuadParticle {
	private static final int HANG = 6;
	private final SpriteSet sprites;

	WheyDripParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
		super(level, x, y, z, sprites.first());
		this.sprites = sprites;
		this.xd = 0;
		this.yd = 0;
		this.zd = 0;
		this.hasPhysics = false;
		this.gravity = 0;
		this.lifetime = HANG + 4 + random.nextInt(2);   // falls 0.2 to 0.3 of a block (gravity 0.5), to the base plate
		this.quadSize = 0.08F + random.nextFloat() * 0.03F;
		float k = 0.94F + random.nextFloat() * 0.06F;
		setColor(0.89F * k, 0.91F * k, 0.62F * k);
		setAlpha(0.75F);
		setSprite(sprites.get(0, 1));
	}

	@Override
	public void tick() {
		if (age == HANG) {
			gravity = 0.5F;
			setSprite(sprites.get(1, 1));
		}
		super.tick();
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
				RandomSource random) {
			return new WheyDripParticle(level, x, y, z, sprites);
		}
	}
}
