package com.lordmau5.ffs.util;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.block.abstracts.AbstractBlockValve;
import com.lordmau5.ffs.tile.abstracts.AbstractTankValve;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

/**
 * Created by Lordmau5 on 06.10.2016.
 */
public class TankManager {
    private final SafeWeakHashMap<Integer, SafeWeakHashMap<BlockPos, TreeMap<Integer, List<LayerBlockPos>>>> valveToFrameBlocks = new SafeWeakHashMap<>();
    private final SafeWeakHashMap<Integer, SafeWeakHashMap<BlockPos, TreeMap<Integer, List<LayerBlockPos>>>> valveToAirBlocks = new SafeWeakHashMap<>();
    private final SafeWeakHashMap<Integer, SafeWeakHashMap<BlockPos, BlockPos>> frameBlockToValve = new SafeWeakHashMap<>();
    private final SafeWeakHashMap<Integer, SafeWeakHashMap<BlockPos, BlockPos>> airBlockToValve = new SafeWeakHashMap<>();

    private final SafeWeakHashMap<Integer, List<BlockPos>> blocksToCheck = new SafeWeakHashMap<>();

    public TankManager() {
    }

    private int getDimensionSafely(World world) {
        return getDimensionSafely(world.provider.getDimension());
    }

    private int getDimensionSafely(int dimensionId) {
        /* This is no longer useful if SafeWeakHashMap work as expected
        valveToFrameBlocks.putIfAbsent(dimensionId, new SafeWeakHashMap<>());
        valveToAirBlocks.putIfAbsent(dimensionId, new SafeWeakHashMap<>());
        frameBlockToValve.putIfAbsent(dimensionId, new SafeWeakHashMap<>());
        airBlockToValve.putIfAbsent(dimensionId, new SafeWeakHashMap<>());
        blocksToCheck.putIfAbsent(dimensionId, new ArrayList<>());
        */
        return dimensionId;
    }

    public void add(World world, BlockPos valvePos, TreeMap<Integer, List<LayerBlockPos>> airBlocks, TreeMap<Integer, List<LayerBlockPos>> frameBlocks) {
        if ( airBlocks.isEmpty() ) {
            return;
        }

        TileEntity tile = world.getTileEntity(valvePos);
        if ( tile == null || !(tile instanceof AbstractTankValve) ) {
            return;
        }

        if ( !((AbstractTankValve) tile).isMaster() ) {
            return;
        }

        addIgnore(world.provider.getDimension(), valvePos, airBlocks, frameBlocks);
    }

    public void addIgnore(int dimensionId, BlockPos valvePos, TreeMap<Integer, List<LayerBlockPos>> airBlocks, TreeMap<Integer, List<LayerBlockPos>> frameBlocks) {
        dimensionId = getDimensionSafely(dimensionId);

        valveToAirBlocks.get(dimensionId).put(valvePos, airBlocks);
        for (int layer : airBlocks.keySet()) {
            for (LayerBlockPos pos : airBlocks.get(layer)) {
                airBlockToValve.get(dimensionId).put(pos, valvePos);
            }
        }

        valveToFrameBlocks.get(dimensionId).put(valvePos, frameBlocks);
        for (int layer : frameBlocks.keySet()) {
            for (LayerBlockPos pos : frameBlocks.get(layer)) {
                frameBlockToValve.get(dimensionId).put(pos, valvePos);
            }
        }
    }

    public void remove(int dimensionId, BlockPos valve) {
        dimensionId = getDimensionSafely(dimensionId);

        airBlockToValve.get(dimensionId).values().removeAll(Collections.singleton(valve));
        valveToAirBlocks.get(dimensionId).remove(valve);

        frameBlockToValve.get(dimensionId).values().removeAll(Collections.singleton(valve));
        valveToFrameBlocks.get(dimensionId).remove(valve);
    }

    public void removeAllForDimension(int dimensionId) {
        dimensionId = getDimensionSafely(dimensionId);

        valveToAirBlocks.get(dimensionId).clear();
        valveToFrameBlocks.get(dimensionId).clear();
        airBlockToValve.get(dimensionId).clear();
        frameBlockToValve.get(dimensionId).clear();
        blocksToCheck.get(dimensionId).clear();
    }

    public AbstractTankValve getValveForBlock(World world, BlockPos pos) {
        if ( !isPartOfTank(world, pos) ) {
            return null;
        }

        int dimensionId = getDimensionSafely(world);

        TileEntity tile = null;
        if ( frameBlockToValve.get(dimensionId).containsKey(pos) ) {
            tile = world.getTileEntity(frameBlockToValve.get(dimensionId).get(pos));
        } else if ( airBlockToValve.get(dimensionId).containsKey(pos) ) {
            tile = world.getTileEntity(airBlockToValve.get(dimensionId).get(pos));
        }

        return tile instanceof AbstractTankValve ? (AbstractTankValve) tile : null;
    }

    public List<BlockPos> getFrameBlocksForValve(AbstractTankValve valve) {
        int dimensionId = getDimensionSafely(valve.getWorld());

        List<BlockPos> blocks = new ArrayList<>();
        if ( valveToFrameBlocks.get(dimensionId).containsKey(valve.getPos()) ) {
            for (int layer : valveToFrameBlocks.get(dimensionId).get(valve.getPos()).keySet()) {
                blocks.addAll(valveToFrameBlocks.get(dimensionId).get(valve.getPos()).get(layer));
            }
        }

        return blocks;
    }

    public TreeMap<Integer, List<LayerBlockPos>> getAirBlocksForValve(AbstractTankValve valve) {
        int dimensionId = getDimensionSafely(valve.getWorld());

        if ( valveToAirBlocks.get(dimensionId).containsKey(valve.getPos()) ) {
            return valveToAirBlocks.get(dimensionId).get(valve.getPos());
        }

        return null;
    }

    public boolean isValveInLists(World world, AbstractTankValve valve) {
        int dimensionId = getDimensionSafely(world);

        return valveToAirBlocks.get(dimensionId).containsKey(valve.getPos());
    }

    public boolean isPartOfTank(World world, BlockPos pos) {
        int dimensionId = getDimensionSafely(world);

        return frameBlockToValve.get(dimensionId).containsKey(pos) || airBlockToValve.get(dimensionId).containsKey(pos);
    }

    @SubscribeEvent
    public void entityJoinWorld(EntityJoinWorldEvent event) {
        if ( event.getWorld() == null || event.getEntity() == null ) {
            return;
        }

        if ( !(event.getEntity() instanceof EntityCreature) ) {
            return;
        }

        if ( isPartOfTank(event.getWorld(), event.getEntity().getPosition()) ) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.WorldTickEvent event) {
        if ( event.world.isRemote ) {
            return;
        }

        if ( event.phase != TickEvent.Phase.END ) {
            return;
        }

        int dimensionId = event.world.provider.getDimension();
        if ( blocksToCheck.isEmpty() || blocksToCheck.get(dimensionId) == null || blocksToCheck.get(dimensionId).isEmpty() ) {
            return;
        }

        AbstractTankValve valve;
        for (BlockPos pos : blocksToCheck.get(dimensionId)) {
            if ( isPartOfTank(event.world, pos) ) {
                valve = getValveForBlock(event.world, pos);
                if ( valve != null ) {
                    if ( !GenericUtil.isValidTankBlock(event.world, pos, event.world.getBlockState(pos), GenericUtil.getInsideForTankFrame(valve.getAirBlocks(), pos)) ) {
                        valve.breakTank();
                        break;
                    }
                }
            }
        }
        blocksToCheck.get(dimensionId).clear();
    }

    private void addBlockForCheck(World world, BlockPos pos) {
        int dimensionId = getDimensionSafely(world);
        List<BlockPos> blocks = blocksToCheck.get(dimensionId);
        if ( blocks == null ) {
            blocks = new ArrayList<>();
        }

        blocks.add(pos);
        blocksToCheck.put(dimensionId, blocks);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();

        if ( world.isRemote ) {
            return;
        }

        if ( !isPartOfTank(world, pos) ) {
            return;
        }

        addBlockForCheck(world, pos);
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();

        if ( world.isRemote ) {
            return;
        }

        if ( !isPartOfTank(world, pos) ) {
            return;
        }

        addBlockForCheck(world, pos);
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        World world = event.getWorld();
        EntityPlayer player = event.getEntityPlayer();

        if ( player == null ) {
            return;
        }

        if ( !isPartOfTank(world, pos) ) {
            return;
        }

        if ( event.getHand() == EnumHand.OFF_HAND ) {
            event.setCanceled(true);
            return;
        }

        if ( player.getHeldItemOffhand() != ItemStack.EMPTY ) {
            if ( player.getHeldItemOffhand().getItem() == FancyFluidStorage.itemTit ) {
                return;
            }
        }

        if ( world.isRemote ) {
            player.swingArm(EnumHand.MAIN_HAND);
        }

        if ( world.getBlockState(pos).getBlock() instanceof AbstractBlockValve ) {
            return;
        }

        event.setCanceled(true);

        if ( player.isSneaking() ) {
            ItemStack mainHand = player.getHeldItemMainhand();
            if ( mainHand != ItemStack.EMPTY ) {
                if ( player.isCreative() ) {
                    mainHand = mainHand.copy();
                }
                mainHand.onItemUse(player, world, pos, EnumHand.MAIN_HAND, event.getFace(), (float) event.getHitVec().x, (float) event.getHitVec().y, (float) event.getHitVec().z);
            }
            return;
        }

        AbstractTankValve tile = getValveForBlock(world, pos);
        if ( tile != null && tile.getMasterValve() != null ) {
            AbstractTankValve valve = tile.getMasterValve();
            if ( valve.isValid() ) {
                if ( GenericUtil.isFluidContainer(event.getItemStack()) ) {
                    if ( GenericUtil.fluidContainerHandler(world, valve, player) ) {
                        valve.markForUpdateNow();
                    }
                } else {
                    player.openGui(FancyFluidStorage.INSTANCE, 1, tile.getWorld(), tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ());
                }
            }
        }
    }

    @SubscribeEvent
    public void onFillBucket(FillBucketEvent event) {
        if ( event.getEntityPlayer().isSneaking() ) {
            return;
        }

        if ( event.getTarget() == null ) {
            return;
        }

        BlockPos pos = event.getTarget().getBlockPos();

        if ( !isPartOfTank(event.getWorld(), pos) ) {
            return;
        }

        event.setCanceled(true);
    }

}
