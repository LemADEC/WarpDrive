package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CelestialObjectManager;

import javax.annotation.Nonnull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.BlockStateArgument;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.thread.EffectiveSide;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class CommandDebug {
	
	private static class DimensionNameArgument implements ArgumentType<String> {
		
		private static final Collection<String> EXAMPLES = Arrays.asList("overworld", "nether", "end", "theend",
		                                                                 "space", "hyper", "hyperspace",
		                                                                 "<dimensionId>", "<dimensionName>");
		
		@Nonnull
		public static DimensionNameArgument create() {
			return new DimensionNameArgument();
		}
		
		@Nonnull
		public static String get(@Nonnull final CommandContext<CommandSource> context, @Nonnull final String name) throws CommandSyntaxException {
			return context.getArgument(name, String.class);
		}
		
		@Nonnull
		@Override
		public String parse(@Nonnull final StringReader stringReader) throws CommandSyntaxException {
			return stringReader.getString();
		}
		
		public Collection<String> getExamples() {
			return EXAMPLES;
		}
	}
	
	public static void register(@Nonnull final CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(
				Commands.literal("wdebug")
				        .requires(commandSource -> commandSource.hasPermissionLevel(2))
				        .then(Commands.argument("dimension", DimensionNameArgument.create())
				                      .then(Commands.argument("blockPos", BlockPosArgument.blockPos())
				                                    .then(Commands.argument("blockState", BlockStateArgument.blockState())
				                                                  .then(Commands.argument("actionSequence", StringArgumentType.string())
				                                                                .executes((commandContext) -> execute(commandContext.getSource(),
				                                                                                                      DimensionNameArgument.get(commandContext, "dimension"),
				                                                                                                      BlockPosArgument.getBlockPos(commandContext, "blockPos"),
				                                                                                                      BlockStateArgument.getBlockState(commandContext, "blockState").getState(),
				                                                                                                      StringArgumentType.getString(commandContext, "actionSequence")))))))
				        
				        .then(Commands.literal("help")
				                      .executes((commandContext) -> help(commandContext.getSource(),
				                                                         commandContext.getNodes().get(0).getNode().getName() ))
				             )
				        .executes((commandContext) -> help(commandContext.getSource(),
				                                           commandContext.getNodes().get(0).getNode().getName() )
				                 )
		                   );
	}
	
	private static int help(@Nonnull final CommandSource commandSource, @Nonnull final String name) {
		commandSource.sendFeedback(new StringTextComponent("/" + name + " <dimension> <x> <y> <z> <blockId> <action><action>...\n"
				+ "dimension: 0/world, 2/space, 3/hyperspace\n"
				+ "coordinates: x,y,z\n"
				+ "action: I(nvalidate), V(alidate), A(set air), R(emoveEntity), P(setBlock), S(etEntity), L(oad), U(nload), C(updateBlockInfo)"), false);
		return 0;
	}
	
	private static int execute(@Nonnull final CommandSource commandSource,
	                    @Nonnull final String stringDimension,
	                    @Nonnull final BlockPos blockPos,
	                    @Nonnull final BlockState blockState,
	                    @Nonnull final String actions) {
		
		final DimensionType dimensionType;
		try {
			dimensionType = DimensionType.byName(Objects.requireNonNull(CelestialObjectManager.getDimensionId(stringDimension, commandSource.asPlayer())));
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			commandSource.sendErrorMessage(new TranslationTextComponent("warpdrive.command.undefined_dimension",
			                                                            stringDimension).setStyle(Commons.getStyleWarning()) );
			return 0;
		}
		
		WarpDrive.logger.info(String.format("Execute actions %s %s",
		                                    actions, Commons.format(dimensionType, blockPos)));
		final World world = Commons.getOrCreateWorldServer(dimensionType);
		if (world == null) {
			commandSource.sendErrorMessage(new TranslationTextComponent("warpdrive.command.undefined_dimension",
			                                                            Commons.format(dimensionType)).setStyle(Commons.getStyleWarning()) );
			return 0;
		}
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		WarpDrive.logger.info(String.format("CommandDebug %s, Current blockState is %s, tile entity is %s",
		                                    Commons.format(world),
		                                    world.getBlockState(blockPos), ((tileEntity == null) ? "undefined" : "defined")));
		final String side = EffectiveSide.get() == LogicalSide.CLIENT ? "Client" : "Server";
		
		// I(nvalidate), V(alidate), A(set air), R(emoveEntity), P(setBlock), S(etEntity), U(nload), L(oad)
		boolean bReturn;
		for (final char cAction : actions.toUpperCase().toCharArray()) {
			switch (cAction) {
			case 'I':
				WarpDrive.logger.info(String.format("CommandDebug %s: invalidating",
				                                    side));
				if (tileEntity != null) {
					tileEntity.remove();
				}
				break;
			case 'V':
				WarpDrive.logger.info(String.format("CommandDebug %s: validating",
				                                    side));
				if (tileEntity != null) {
					tileEntity.validate();
				}
				break;
			case 'A':
				WarpDrive.logger.info(String.format("CommandDebug %s: setting to Air",
				                                    side));
				bReturn = world.removeBlock(blockPos, false);
				WarpDrive.logger.info(String.format("CommandDebug %s: return value is %s",
				                                    side, bReturn));
				break;
			case 'R':
				WarpDrive.logger.info(String.format("CommandDebug %s: removing tile entity",
				                                    side));
				world.removeTileEntity(blockPos);
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
				WarpDrive.logger.info(String.format("CommandDebug %s: setting blockState %s" ,
				                                    side, blockState));
				bReturn = world.setBlockState(blockPos, blockState, cAction - '0');
				WarpDrive.logger.info(String.format("CommandDebug %s: return value is %s",
				                                    side, bReturn));
				break;
			case 'P':
				WarpDrive.logger.info(String.format("CommandDebug %s: setting blockState %s",
				                                    side, blockState));
				bReturn = world.setBlockState(blockPos, blockState, 2);
				WarpDrive.logger.info(String.format("CommandDebug %s: return value is %s",
				                                    side, bReturn));
				break;
			case 'S':
				WarpDrive.logger.info(String.format("CommandDebug %s: set entity",
				                                    side));
				world.setTileEntity(blockPos, tileEntity);
				break;
			case 'L':
				WarpDrive.logger.info(String.format("CommandDebug %s: loading entity",
				                                    side));
				if (tileEntity != null) {
					tileEntity.onLoad();
				}
				break;
			case 'U':
				WarpDrive.logger.info(String.format("CommandDebug %s: unloading entity",
				                                    side));
				if (tileEntity != null) {
					tileEntity.onChunkUnloaded();
				}
				break;
			case 'C':
				WarpDrive.logger.info(String.format("CommandDebug %s: updating containing block info",
				                                    side));
				if (tileEntity != null) {
					tileEntity.updateContainingBlockInfo();
				}
				break;
			default:
				WarpDrive.logger.info(String.format("CommandDebug %s: invalid step '%s",
				                                    side, cAction));
				break;
			}
		}
		
		return actions.length();
	}

}