package mod.lucky.drop.func;

import java.util.HashMap;

import mod.lucky.drop.DropSingle;
import net.minecraft.nbt.NBTTagCompound;

public abstract class DropFunction {
    public abstract void process(DropProcessData processData);

    public abstract String getType();

    public void registerProperties() {
    }

    public static void registerGlobalProperties() {
        DropSingle.setDefaultProperty("all", "type", String.class, "item");
        DropSingle.setDefaultProperty("all", "ID", String.class, "");
        DropSingle.setDefaultProperty("all", "damage", Integer.class, 0);
        DropSingle.setDefaultProperty("all", "amount", Integer.class, 1);
        DropSingle.setDefaultProperty("all", "reinitialize", Boolean.class, false);
        DropSingle.setDefaultProperty("all", "postDelayInit", Boolean.class, true);
        DropSingle.setDefaultProperty("all", "delay", Float.class, 0);
        DropSingle.setDefaultProperty("all", "posX", Float.class, 0);
        DropSingle.setDefaultProperty("all", "posY", Float.class, 0);
        DropSingle.setDefaultProperty("all", "posZ", Float.class, 0);
        DropSingle.setDefaultProperty("all", "pos", String.class, "(0,0,0)");
        DropSingle.setDefaultProperty("all", "pos2X", Float.class, 0);
        DropSingle.setDefaultProperty("all", "pos2Y", Float.class, 0);
        DropSingle.setDefaultProperty("all", "pos2Z", Float.class, 0);
        DropSingle.setDefaultProperty("all", "pos2", String.class, "(0,0,0)");
        DropSingle.setDefaultProperty("all", "posOffsetX", Float.class, 0);
        DropSingle.setDefaultProperty("all", "posOffsetY", Float.class, 0);
        DropSingle.setDefaultProperty("all", "posOffsetZ", Float.class, 0);
        DropSingle.setDefaultProperty("all", "posOffset", String.class, "(0,0,0)");
        DropSingle.setDefaultProperty("all", "centerX", Integer.class, 0);
        DropSingle.setDefaultProperty("all", "centerY", Integer.class, 0);
        DropSingle.setDefaultProperty("all", "centerZ", Integer.class, 0);
        DropSingle.setDefaultProperty("all", "rotation", Integer.class, 0);
        DropSingle.setDefaultProperty("all", "doUpdate", Boolean.class, false);
        DropSingle.setDefaultProperty("all", "blockMode", String.class, "replace");
        DropSingle.setDefaultProperty("all", "displayCommandOutput", Boolean.class, false);
        DropSingle.setDefaultProperty("all", "commandSender", String.class, "@");
        DropSingle.setDefaultProperty("all", "duration", Integer.class, 200);
        DropSingle.setDefaultProperty("all", "NBTTag", NBTTagCompound.class, null);
    }

    private static HashMap<String, DropFunction> dropFunctions = new HashMap<String, DropFunction>();

    public static void registerFunctions() {
        registerDropFunction(new DropFuncBlock());
        registerDropFunction(new DropFuncCommand());
        registerDropFunction(new DropFuncDifficulty());
        registerDropFunction(new DropFuncEffect());
        registerDropFunction(new DropFuncEntity());
        registerDropFunction(new DropFuncExplosion());
        registerDropFunction(new DropFuncFill());
        registerDropFunction(new DropFuncItem());
        registerDropFunction(new DropFuncMessage());
        registerDropFunction(new DropFuncParticle());
        registerDropFunction(new DropFuncSound());
        registerDropFunction(new DropFuncStructure());
        registerDropFunction(new DropFuncTime());
        registerDropFunction(new DropFuncNothing());
        registerGlobalProperties();
    }

    public static DropFunction getDropFunction(DropSingle dropSingle) {
        return getDropFunction(dropSingle.getPropertyString("type"));
    }

    public static DropFunction getDropFunction(String type) {
        return dropFunctions.get(type);
    }

    public static void registerDropFunction(DropFunction dropFunction) {
        dropFunctions.put(dropFunction.getType(), dropFunction);
        dropFunction.registerProperties();
    }
}
