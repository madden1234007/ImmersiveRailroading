package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.tile.RailBase;
import cam72cam.immersiverailroading.tile.Rail;
import cam72cam.immersiverailroading.tile.RailGag;
import cam72cam.mod.block.*;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.block.tile.TileEntityTickable;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Identifier;
import cam72cam.mod.util.TagCompound;
import cam72cam.mod.world.World;
import trackapi.lib.ITrack;

import java.util.function.Function;

public abstract class BlockRailBase extends BlockTypeTickable {
	BlockRailBase(BlockSettings settings, Function<TileEntity, BlockEntity> constructData) {
		super(settings
                .withConnectable(false)
                .withMaterial(Material.METAL)
                .withHardness(1F),
                constructData
        );
	}

	/*

	Custom BlockType for IBreakCancellable

	 */

    protected class RailBlockInternal extends BlockTypeInternal implements IBreakCancelable {
        @Override
        public boolean tryBreak(World world, Vec3i pos, Player player) {
            return BlockRailBase.this.tryBreak(world, pos, player);
        }
    }

    @Override
    protected BlockInternal getBlock() {
        return new RailBlockInternal();
    }

    /*

    Custom Tile for ITrack

     */

    public static class TileEntityRailBlock extends TileEntityTickable implements ITrack {

        public TileEntityRailBlock() {
            super();
        }

        public TileEntityRailBlock(Identifier id) {
            super(id);
        }

        @Override
        public double getTrackGauge() {
            return instance() instanceof RailBase ? ((RailBase)instance()).getTrackGauge() : 0;
        }

        @Override
        public net.minecraft.util.math.Vec3d getNextPosition(net.minecraft.util.math.Vec3d pos, net.minecraft.util.math.Vec3d mot) {
            return instance() instanceof RailBase ? ((RailBase)instance()).getNextPosition(new Vec3d(pos), new Vec3d(mot)).internal : pos;
        }

        @Override
        public Identifier getName() {
            return new Identifier(ImmersiveRailroading.MODID, "tile_track");
        }
    }

    @Override
    public TileEntity getTile() {
        return new TileEntityRailBlock(id);
    }

    /*

    Helpers

     */


	public static void breakParentIfExists(RailBase te) {
		Rail parent = te.getParentTile();
		if (parent != null && !te.getWillBeReplaced()) {
            parent.spawnDrops();
            //if (tryBreak(te.getWorld(), te.getPos())) {
            te.world.setToAir(parent.pos);
            //}
		}
	}

    public boolean tryBreak(World world, Vec3i pos, Player player) {
        try {
            RailBase rail = world.getBlockEntity(pos, RailBase.class);
            if (rail != null) {
                if (rail.getReplaced() != null) {
                    // new object here is important
                    RailGag newGag = (RailGag) world.reconstituteBlockEntity(rail.getReplaced());

                    while(true) {
                        if (newGag.getParent() != null && world.hasBlockEntity(newGag.getParent(), Rail.class)) {
                            rail.world.internal.setTileEntity(pos.internal, newGag.internal);
                            newGag.markDirty();
                            breakParentIfExists(rail);
                            return false;
                        }
                        // Only do replacement if parent still exists

                        TagCompound data = newGag.getReplaced();
                        if (data == null) {
                            break;
                        }

                        newGag = (RailGag) world.reconstituteBlockEntity(data);
                    }
                }
            }
        } catch (StackOverflowError ex) {
            ImmersiveRailroading.error("Invalid recursive rail block at %s", pos);
            ImmersiveRailroading.catching(ex);
            world.setToAir(pos);
        }
        return true;
    }
}
