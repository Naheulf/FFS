package com.lordmau5.ffs.tile.interfaces;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Created by Dustin on 22.01.2016.
 */
public interface INameableTile {

    default String getTileName() {
        return "";
    }

    void setTileName(String name);

    default void saveTileNameToNBT(NBTTagCompound tag) {
        if ( !getTileName().isEmpty() ) {
            tag.setString("tile_name", getTileName());
        }
    }

    default void readTileNameFromNBT(NBTTagCompound tag) {
        if ( tag.hasKey("tile_name") ) {
            setTileName(tag.getString("tile_name"));
        }
    }

}
