package dev.farmings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.farmings.conveyor.ConveyorDriveEntity;
import dev.farmings.conveyor.ConveyorKind;
import dev.farmings.conveyor.ConveyorPieceBlock;
import dev.farmings.farm.conveyor.Travel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draws what rides a belt (docs/conveyors.md, "Items drawn"): each parcel as its item lying on the belt, placed along the run from the
 * drive's parcel list and moved on smoothly between the server's updates. Slopes lift it.
 */
public class BeltItemsRenderer implements BlockEntityRenderer<ConveyorDriveEntity, BeltItemsRenderer.State> {
	public static final class State extends BlockEntityRenderState {
		final List<ItemStackRenderState> items = new ArrayList<>();
		final List<Vec3> at = new ArrayList<>();
		final List<Integer> light = new ArrayList<>();
		float yaw;
	}

	private static final double SURFACE = 0.25;
	private final ItemModelResolver resolver;

	public BeltItemsRenderer(BlockEntityRendererProvider.Context ctx) {
		this.resolver = ctx.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ConveyorDriveEntity drive, State state, float partial, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
		BlockEntityRenderer.super.extractRenderState(drive, state, partial, camera, crumbling);
		state.items.clear();
		state.at.clear();
		state.light.clear();
		if (drive.getLevel() == null || drive.kind() != ConveyorKind.BELT) {
			return;
		}
		Direction d = drive.getBlockState().getValue(dev.farmings.conveyor.ConveyorDriveBlock.FACING);
		state.yaw = d.toYRot();
		List<BlockPos> cells = drive.cells();
		if (cells.isEmpty() || drive.getLevel().getGameTime() % 40 == 0) {
			drive.refresh();     // the client traces the run itself; only the load comes from the server
			cells = drive.cells();
		}
		if (cells.isEmpty() || drive.parcels().isEmpty()) {
			return;
		}
		double moved = drive.syncPowered() ? drive.kind().metresPerSecond / 20.0 * (drive.getLevel().getGameTime() - drive.syncTick() + partial) : 0;
		BlockPos origin = drive.getBlockPos();
		int seed = 0;
		for (Travel.Parcel<ItemStack> p : drive.parcels()) {
			double t = Math.max(0, p.toGo() - moved);
			Vec3 w = place(drive, cells, d, t);
			if (w == null) {
				continue;
			}
			ItemStackRenderState r = new ItemStackRenderState();
			resolver.updateForTopItem(r, p.load(), ItemDisplayContext.FIXED, drive.getLevel(), null, seed++);
			state.items.add(r);
			state.at.add(w.subtract(origin.getX(), origin.getY(), origin.getZ()));
			// the light where the item lies, not inside the drive block
			state.light.add(net.minecraft.util.LightCoordsUtil.getLightCoords(drive.getLevel(), BlockPos.containing(w.x, w.y + 0.2, w.z)));
		}
	}

	/** Where a parcel {@code toGo} metres short of the head end lies: between the head and tail edges of its cell, lifted on slopes. */
	static Vec3 place(ConveyorDriveEntity drive, List<BlockPos> cells, Direction d, double toGo) {
		double before = 0;
		for (BlockPos c : cells) {
			BlockState s = drive.getLevel().getBlockState(c);
			double len = ConveyorPieceBlock.length(s);
			if (toGo <= before + len + 1e-9) {
				double f = len <= 0 ? 0 : (toGo - before) / len;     // 0 at the head edge, 1 at the tail edge
				ConveyorPieceBlock.Slope slope = s.getBlock() instanceof ConveyorPieceBlock ? s.getValue(ConveyorPieceBlock.SLOPE) : ConveyorPieceBlock.Slope.FLAT;
				double yHead = c.getY() + SURFACE + (slope == ConveyorPieceBlock.Slope.UP ? 1 : 0);
				double yTail = c.getY() + SURFACE + (slope == ConveyorPieceBlock.Slope.DOWN ? 1 : 0);
				Vec3 centre = Vec3.atBottomCenterOf(c);
				Vec3 head = new Vec3(centre.x + d.getStepX() * 0.5, yHead, centre.z + d.getStepZ() * 0.5);
				Vec3 tail = new Vec3(centre.x - d.getStepX() * 0.5, yTail, centre.z - d.getStepZ() * 0.5);
				return head.add(tail.subtract(head).scale(f));
			}
			before += len;
		}
		return null;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		for (int i = 0; i < state.items.size(); i++) {
			ItemStackRenderState r = state.items.get(i);
			if (r.isEmpty()) {
				continue;
			}
			Vec3 a = state.at.get(i);
			pose.pushPose();
			pose.translate(a.x, a.y, a.z);
			pose.rotateDegrees(Axis.YP, -state.yaw);
			pose.rotateDegrees(Axis.XP, 90);
			pose.scale(0.6F, 0.6F, 0.6F);
			r.submit(pose, collector, state.light.get(i), OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}
}
