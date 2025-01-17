package com.lordmau5.ffs.block.abstracts;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.tile.abstracts.AbstractTankValve;
import com.lordmau5.ffs.util.GenericUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Created by Dustin on 08.02.2016.
 */
public abstract class AbstractBlockValve extends Block {

    protected AbstractBlockValve(String name) {
        super(Material.IRON);

        setTranslationKey(FancyFluidStorage.MODID + "." + name);
        setRegistryName(name);
        setCreativeTab(CreativeTabs.REDSTONE);
        setHardness(5.0F);
        setResistance(10.0F);

        setDefaultState();
    }

    protected abstract void setDefaultState();

    public abstract BlockStateContainer createBlockState();

    public abstract IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos);

    @Override
    public float getBlockHardness(IBlockState blockState, World worldIn, BlockPos pos) {
        TileEntity tile = worldIn.getTileEntity(pos);
        if ( tile instanceof AbstractTankValve ) {
            AbstractTankValve valve = (AbstractTankValve) tile;
            if ( valve.isValid() && valve.getTankConfig().getFluidStack() != null ) {
                return 20.0F;
            }
        }
        return 5.0F;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        TileEntity tile = world.getTileEntity(pos);
        if ( tile instanceof AbstractTankValve ) {
            AbstractTankValve valve = (AbstractTankValve) world.getTileEntity(pos);
            if ( valve != null && valve.isValid() ) {
                valve.breakTank();
            }
        }
        super.onExplosionDestroy(world, pos, explosion);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if ( !world.isRemote ) {
            AbstractTankValve valve = (AbstractTankValve) world.getTileEntity(pos);
            if ( valve != null && valve.isValid() ) {
                valve.breakTank();
            }
        }

        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if ( !world.isRemote ) {
            AbstractTankValve valve = (AbstractTankValve) world.getTileEntity(pos);
            if ( valve != null && valve.isValid() ) {
                valve.breakTank();
            }
        }

        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if ( player.isSneaking() ) return false;

        AbstractTankValve valve = (AbstractTankValve) world.getTileEntity(pos);
        if ( valve == null ) {
            return true;
        }

        if ( valve.isValid() ) {
            if ( GenericUtil.isFluidContainer(player.getHeldItemMainhand()) ) {
                if ( GenericUtil.fluidContainerHandler(world, valve, player) ) {
                    valve.markForUpdateNow();
                    return true;
                }
            }

            player.openGui(FancyFluidStorage.INSTANCE, 0, world, pos.getX(), pos.getY(), pos.getZ());
            return true;
        } else {
            valve.buildTank_player(player, facing.getOpposite());
        }
        return true;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(this);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if ( te instanceof AbstractTankValve ) {
            AbstractTankValve valve = (AbstractTankValve) te;
            return valve.getComparatorOutput();
        }
        return 0;
    }

    @Override
    public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, EntityLiving.SpawnPlacementType type) {
        return false;
    }

}
