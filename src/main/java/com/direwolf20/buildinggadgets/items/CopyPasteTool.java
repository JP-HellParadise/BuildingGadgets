package com.direwolf20.buildinggadgets.items;

import com.direwolf20.buildinggadgets.BuildingGadgets;
import com.direwolf20.buildinggadgets.Config;
import com.direwolf20.buildinggadgets.entities.BlockBuildEntity;
import com.direwolf20.buildinggadgets.tools.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.direwolf20.buildinggadgets.tools.GadgetUtils.withSuffix;

public class CopyPasteTool extends GenericGadget {

    public enum toolModes {
        Copy, Paste;
        private static toolModes[] vals = values();

        public toolModes next() {
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }

    public CopyPasteTool() {
        setRegistryName("copypastetool");        // The unique name (within your mod) that identifies this item
        setUnlocalizedName(BuildingGadgets.MODID + ".copypastetool");     // Used for localization (en_US.lang)
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.TOOLS);
    }

    public static void setAnchor(ItemStack stack, BlockPos anchorPos) {
        GadgetUtils.writePOSToNBT(stack, anchorPos, "anchor");
    }

    public static BlockPos getAnchor(ItemStack stack) {
        return GadgetUtils.getPOSFromNBT(stack, "anchor");
    }

    public static void setLastBuild(ItemStack stack, BlockPos anchorPos) {
        GadgetUtils.writePOSToNBT(stack, anchorPos, "lastBuild");
    }

    public static BlockPos getLastBuild(ItemStack stack) {
        return GadgetUtils.getPOSFromNBT(stack, "lastBuild");
    }

    public static void setStartPos(ItemStack stack, BlockPos startPos) {
        GadgetUtils.writePOSToNBT(stack, startPos, "startPos");
    }

    public static BlockPos getStartPos(ItemStack stack) {
        return GadgetUtils.getPOSFromNBT(stack, "startPos");
    }

    public static void setEndPos(ItemStack stack, BlockPos startPos) {
        GadgetUtils.writePOSToNBT(stack, startPos, "endPos");
    }

    public static BlockPos getEndPos(ItemStack stack) {
        return GadgetUtils.getPOSFromNBT(stack, "endPos");
    }

    public static void addBlockToMap(ItemStack stack, BlockMap map) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
        }
        NBTTagList blocks = (NBTTagList) tagCompound.getTag("blocksMapList");
        if (blocks == null) {
            blocks = new NBTTagList();
        }
        NBTTagList MapIntStateTag = (NBTTagList) tagCompound.getTag("mapIntState");
        if (MapIntStateTag == null) {
            MapIntStateTag = new NBTTagList();
        }
        BlockPos startPos = getStartPos(stack);
        int px = (((map.pos.getX() - startPos.getX()) & 0xff) << 16);
        int py = (((map.pos.getY() - startPos.getY()) & 0xff) << 8);
        int pz = (((map.pos.getZ() - startPos.getZ()) & 0xff));
        int p = (px + py + pz);

        NBTTagCompound blockMap = new NBTTagCompound();
        BlockMapIntState MapIntState = new BlockMapIntState();
        MapIntState.getIntStateMapFromNBT(MapIntStateTag);
        MapIntState.addToMap(map.state);

        blockMap.setShort("state", MapIntState.findSlot(map.state));
        blockMap.setInteger("pos", p);
        blocks.appendTag(blockMap);
        tagCompound.setTag("blocksMapList", blocks);
        tagCompound.setTag("mapIntState", MapIntState.putIntStateMapIntoNBT());
        stack.setTagCompound(tagCompound);
    }

    public static ArrayList<BlockMap> getBlockMapList(ItemStack stack, BlockPos startBlock) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        ArrayList<BlockMap> blockMap = new ArrayList<BlockMap>();
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
        }
        NBTTagList blocks = (NBTTagList) tagCompound.getTag("blocksMapList");
        if (blocks == null) {
            blocks = new NBTTagList();
        }
        if (blocks.tagCount() == 0) {
            return blockMap;
        }
        NBTTagList MapIntStateTag = (NBTTagList) tagCompound.getTag("mapIntState");
        if (MapIntStateTag == null) {
            MapIntStateTag = new NBTTagList();
        }
        BlockMapIntState MapIntState = new BlockMapIntState();
        MapIntState.getIntStateMapFromNBT(MapIntStateTag);
        for (int i = 0; i < blocks.tagCount(); i++) {
            NBTTagCompound compound = blocks.getCompoundTagAt(i);
            int p = compound.getInteger("pos");
            int x = startBlock.getX() + (int) (byte) ((p & 0xff0000) >> 16);
            int y = startBlock.getY() + (int) (byte) ((p & 0x00ff00) >> 8);
            int z = startBlock.getZ() + (int) (byte) (p & 0x0000ff);
            short IntState = compound.getShort("state");
            blockMap.add(new BlockMap(new BlockPos(x, y, z), MapIntState.getStateFromSlot(IntState), (int) (byte) ((p & 0xff0000) >> 16), (int) (byte) ((p & 0x00ff00) >> 8), (int) (byte) (p & 0x0000ff)));
        }
        return blockMap;
    }

    public static BlockMapIntState getBlockMapIntState(ItemStack stack) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
        }
        NBTTagList MapIntStateTag = (NBTTagList) tagCompound.getTag("mapIntState");
        if (MapIntStateTag == null) {
            MapIntStateTag = new NBTTagList();
        }
        BlockMapIntState MapIntState = new BlockMapIntState();
        MapIntState.getIntStateMapFromNBT(MapIntStateTag);
        return MapIntState;
    }

    public static void resetBlockMap(ItemStack stack) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
        }
        NBTTagList blocks = new NBTTagList();
        NBTTagList blocksIntMap = new NBTTagList();
        tagCompound.setTag("blocksMapList", blocks);
        tagCompound.setTag("mapIntState", blocksIntMap);
        stack.setTagCompound(tagCompound);
    }

    public static void setToolMode(ItemStack stack, toolModes mode) {
        //Store the tool's mode in NBT as a string
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
        }
        tagCompound.setString("mode", mode.name());
        stack.setTagCompound(tagCompound);
    }

    public static toolModes getToolMode(ItemStack stack) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        toolModes mode = toolModes.Copy;
        if (tagCompound == null) {
            setToolMode(stack, mode);
            return mode;
        }
        try {
            mode = toolModes.valueOf(tagCompound.getString("mode"));
        } catch (Exception e) {
            setToolMode(stack, mode);
        }
        return mode;
    }

    @Override
    public void addInformation(ItemStack stack, World player, List<String> list, ITooltipFlag b) {
        long time = System.nanoTime();
        //Add tool information to the tooltip
        super.addInformation(stack, player, list, b);
        list.add(TextFormatting.AQUA + I18n.format("tooltip.gadget.mode") + ": " + getToolMode(stack));
        if (Config.poweredByFE) {
            IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
            list.add(TextFormatting.WHITE + I18n.format("tooltip.gadget.energy") + ": " + withSuffix(energy.getEnergyStored()) + "/" + withSuffix(energy.getMaxEnergyStored()));
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            Map<Short, IBlockState> IntStateMap = getBlockMapIntState(stack).getIntStateMap();
            ArrayList<BlockMap> blockMapList = getBlockMapList(stack, getStartPos(stack));
            Map<IBlockState, String> stateItemMap = new HashMap<IBlockState, String>();
            Map<String, ItemStack> stackNames = new HashMap<String, ItemStack>();
            for (Map.Entry<Short, IBlockState> entry : IntStateMap.entrySet()) {
                IBlockState setBlock = entry.getValue();
                ItemStack itemStack;
                if (entry.getValue().getBlock().canSilkHarvest(player, null, setBlock, null)) {
                    itemStack = InventoryManipulation.getSilkTouchDrop(setBlock);
                } else {
                    itemStack = new ItemStack(Item.getItemFromBlock(setBlock.getBlock()), 1, setBlock.getBlock().damageDropped(setBlock));
                }
                stateItemMap.put(entry.getValue(), itemStack.getDisplayName());
                stackNames.put(itemStack.getDisplayName(), itemStack);
            }
            int totalCount = 0;
            Map<String, Integer> itemCountMap = new HashMap<String, Integer>();
            for (BlockMap blockMap : blockMapList) {
                String name = stateItemMap.get(blockMap.state);
                if (!name.equals("Air")) {
                    if (itemCountMap.get(name) == null) {
                        itemCountMap.put(name, 1);
                    } else {
                        itemCountMap.put(name, itemCountMap.get(name) + 1);
                    }
                }
            }
            for (Map.Entry<String, Integer> entry : itemCountMap.entrySet()) {
                ItemStack itemStack = stackNames.get(entry.getKey());
                int itemCount = InventoryManipulation.countItem(itemStack, Minecraft.getMinecraft().player);
                if (itemCount >= entry.getValue()) {
                    list.add(TextFormatting.GREEN + I18n.format(entry.getKey() + ": " + entry.getValue()));
                } else {
                    list.add(TextFormatting.RED + I18n.format(entry.getKey() + ": " + entry.getValue() + " (" + (entry.getValue() - itemCount) + ")"));
                }

            }
            //System.out.printf("Counted %d Blocks in %.2f ms%n", blockMapList.size(), (System.nanoTime() - time) * 1e-6);
        }
    }

    public void setMode(EntityPlayer player, ItemStack heldItem, int modeInt) {
        //Called when we specify a mode with the radial menu
        toolModes mode = toolModes.values()[modeInt];
        setToolMode(heldItem, mode);
        player.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + new TextComponentTranslation("message.gadget.toolmode").getUnformattedComponentText() + ": " + mode.name()), true);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        player.setActiveHand(hand);
        if (!world.isRemote) {
            if (getToolMode(stack) == toolModes.Copy) {
                copyBlocks(stack, pos, player, world);
            } else if (getToolMode(stack) == toolModes.Paste) {
                buildBlockMap(world, pos.up(), stack, player);
            }
        }
        NBTTagCompound tagCompound = stack.getTagCompound();
        ByteBuf buf = Unpooled.buffer(16);
        ByteBufUtils.writeTag(buf, tagCompound);
        System.out.println(buf.readableBytes());
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        player.setActiveHand(hand);
        if (!world.isRemote) {
            if (getToolMode(stack) == toolModes.Copy) {
                BlockPos pos = VectorTools.getPosLookingAt(player);
                if (pos == null) return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
                copyBlocks(stack, pos, player, world);
            } else if (getToolMode(stack) == toolModes.Paste) {
                if (getAnchor(stack) == null) {
                    BlockPos pos = VectorTools.getPosLookingAt(player);
                    if (pos == null) return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
                    buildBlockMap(world, pos.up(), stack, player);
                } else {
                    buildBlockMap(world, getAnchor(stack).up(), stack, player);
                }
            }
        }
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }

    public void copyBlocks(ItemStack stack, BlockPos pos, EntityPlayer player, World world) {
        if (player.isSneaking()) {
            setEndPos(stack, pos);
        } else {
            setStartPos(stack, pos);
        }
        if (getStartPos(stack) != null && getEndPos(stack) != null) {
            findBlocks(world, getStartPos(stack), getEndPos(stack), stack);
            ArrayList<BlockMap> blockMapList = getBlockMapList(stack, getStartPos(stack));
            long time = System.nanoTime();
            PasteToolBufferBuilder.addMapToBuffer(blockMapList);
            System.out.printf("Copied %d Blocks to buffer in %.2f ms%n", blockMapList.size(), (System.nanoTime() - time) * 1e-6);
            //System.out.println("Copied " + blockMapList.size() + " blocks in: " + (System.currentTimeMillis() - time));
            player.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + new TextComponentTranslation("message.gadget.copied").getUnformattedComponentText()), true);
        }
    }

    public void findBlocks(World world, BlockPos start, BlockPos end, ItemStack stack) {
        resetBlockMap(stack);
        setLastBuild(stack, null);

        int startX = start.getX();
        int startY = start.getY();
        int startZ = start.getZ();

        int endX = end.getX();
        int endY = end.getY();
        int endZ = end.getZ();

        int iStartX = startX < endX ? startX : endX;
        int iStartY = startY < endY ? startY : endY;
        int iStartZ = startZ < endZ ? startZ : endZ;
        int iEndX = startX < endX ? endX : startX;
        int iEndY = startY < endY ? endY : startY;
        int iEndZ = startZ < endZ ? endZ : startZ;

        for (int x = iStartX; x <= iEndX; x++) {
            for (int y = iStartY; y <= iEndY; y++) {
                for (int z = iStartZ; z <= iEndZ; z++) {
                    BlockPos tempPos = new BlockPos(x, y, z);
                    IBlockState tempState = world.getBlockState(tempPos);
                    if (tempState != Blocks.AIR.getDefaultState() && world.getTileEntity(tempPos) == null) {
                        BlockMap tempMap = new BlockMap(tempPos, tempState);
                        addBlockToMap(stack, tempMap);
                    }
                }
            }
        }
    }

    public void buildBlockMap(World world, BlockPos startPos, ItemStack stack, EntityPlayer player) {
        BlockPos anchorPos = getAnchor(stack);
        ArrayList<BlockMap> blockMapList = new ArrayList<BlockMap>();
        if (anchorPos == null) {
            blockMapList = getBlockMapList(stack, startPos);
            setLastBuild(stack, startPos);
        } else {
            blockMapList = getBlockMapList(stack, anchorPos);
            setLastBuild(stack, anchorPos);
        }
        for (BlockMap blockMap : blockMapList) {
            if (world.getBlockState(blockMap.pos) != Blocks.AIR.getDefaultState()) {
                world.spawnEntity(new BlockBuildEntity(world, blockMap.pos, player, blockMap.state, 3, blockMap.state, false));
            } else {
                world.spawnEntity(new BlockBuildEntity(world, blockMap.pos, player, blockMap.state, 1, blockMap.state, false));
            }
        }
        setAnchor(stack, null);
    }

    public static void anchorBlocks(EntityPlayer player, ItemStack stack) {
        BlockPos currentAnchor = getAnchor(stack);
        if (currentAnchor == null) {
            RayTraceResult lookingAt = VectorTools.getLookingAt(player);
            if (lookingAt == null) {
                return;
            }
            currentAnchor = lookingAt.getBlockPos().up();
            setAnchor(stack, currentAnchor);
            player.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + new TextComponentTranslation("message.gadget.anchorrender").getUnformattedComponentText()), true);
        } else {
            setAnchor(stack, null);
            player.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + new TextComponentTranslation("message.gadget.anchorremove").getUnformattedComponentText()), true);
        }
    }

    public static void undoBuild(EntityPlayer player, ItemStack heldItem) {
        World world = player.world;
        if (world.isRemote) {
            return;
        }
        BlockPos startPos = getLastBuild(heldItem);
        if (startPos == null) {
            return;
        }
        ArrayList<BlockMap> blockMapList = getBlockMapList(heldItem, startPos);
        for (BlockMap blockMap : blockMapList) {
            if (world.getBlockState(blockMap.pos) == blockMap.state) {
                world.spawnEntity(new BlockBuildEntity(world, blockMap.pos, player, blockMap.state, 2, blockMap.state, false));
            }
        }
    }
}
