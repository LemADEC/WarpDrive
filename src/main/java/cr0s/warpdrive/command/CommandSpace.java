package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;

import javax.annotation.Nonnull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.server.ServerWorld;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;

public class CommandSpace {
	
	public static class DimensionNameArgument implements ArgumentType<String> {
		
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
		final CommandNode<CommandSource> commandNode = dispatcher.register(
				Commands.literal("space")
				        .requires(commandSource -> commandSource.hasPermissionLevel(2))
				        .then(Commands.argument("players", EntityArgument.players())
				                      .then(Commands.argument("target", DimensionNameArgument.create())
				                                    .executes((commandContext) -> execute(commandContext.getSource(),
				                                                                          EntityArgument.getPlayers(commandContext, "players"),
				                                                                          DimensionNameArgument.get(commandContext, "target") ))
				                           )
				             )
				        .then(Commands.argument("players", EntityArgument.players())
				                      .executes((commandContext) -> execute(commandContext.getSource(),
				                                                            EntityArgument.getPlayers(commandContext, "players"),
				                                                            "space" ))
				             )
				        .then(Commands.argument("target", DimensionNameArgument.create())
				                      .executes((commandContext) -> execute(commandContext.getSource(),
				                                                            Collections.singleton(commandContext.getSource().asPlayer()),
				                                                            DimensionNameArgument.get(commandContext, "target")))
				             )
				        
				        .then(Commands.literal("help")
				                      .executes((commandContext) -> help(commandContext.getSource(),
				                                                         commandContext.getNodes().get(0).getNode().getName() ))
				             )
				        .executes((commandContext) -> execute(commandContext.getSource(),
				                                              Collections.singleton(commandContext.getSource().asPlayer()),
				                                              "space" ))
		                                                                   );
		dispatcher.register(Commands.literal("tpx").redirect(commandNode));
		dispatcher.register(Commands.literal("tpd").redirect(commandNode));
	}
	
	private static int help(@Nonnull final CommandSource commandSource, @Nonnull final String name) {
		commandSource.sendFeedback(new StringTextComponent("/" + name + " (<playerName>) ([overworld|nether|end|theend|space|hyper|hyperspace|<dimensionId>]"), false);
		return 0;
	}
	
	private static int execute(@Nonnull final CommandSource commandSource,
	                           @Nonnull final Collection<ServerPlayerEntity> serverPlayerEntities,
	                           @Nonnull final String target) {
		assert !serverPlayerEntities.isEmpty();
		// unused language keys?
		// "warpdrive.command.player_not_found"
		
		// parse arguments
		// note: "space" will toggle between overworld and space if no dimension was provided
		ResourceLocation dimensionIdTarget = CelestialObjectManager.getDimensionId(target, serverPlayerEntities.iterator().next());
		
		for (final ServerPlayerEntity serverPlayerEntity : serverPlayerEntities) {
			int xTarget = MathHelper.floor(serverPlayerEntity.getPosX());
			int yTarget = Math.min(255, Math.max(0, MathHelper.floor(serverPlayerEntity.getPosY())));
			int zTarget = MathHelper.floor(serverPlayerEntity.getPosZ());
			final CelestialObject celestialObjectCurrent = CelestialObjectManager.get(serverPlayerEntity.world);
			if (dimensionIdTarget == null) {
				if (celestialObjectCurrent == null) {
					commandSource.sendErrorMessage(new TranslationTextComponent("warpdrive.command.player_in_unknown_dimension",
					                                                            serverPlayerEntity.getName().getFormattedText(),
					                                                            serverPlayerEntity.world.getDimension().getType().getRegistryName() )
							                               .setStyle(Commons.getStyleWarning()));
					commandSource.sendFeedback(new TranslationTextComponent("warpdrive.command.specify_explicit_dimension")
							                           .setStyle(Commons.getStyleCorrect()), false);
					continue;
				}
				if ( celestialObjectCurrent.isSpace()
				  || celestialObjectCurrent.isHyperspace() ) {
					// in space or hyperspace => move to closest child
					final CelestialObject celestialObjectChild = CelestialObjectManager.getClosestChild(serverPlayerEntity.world, (int) serverPlayerEntity.getPosX(), (int) serverPlayerEntity.getPosZ());
					//noinspection StatementWithEmptyBody
					if (celestialObjectChild == null) {
						// no operation
					} else if (celestialObjectChild.isVirtual()) {
						commandSource.sendErrorMessage(new TranslationTextComponent("warpdrive.command.player_can_t_go_virtual",
						                                                            serverPlayerEntity.getName().getFormattedText(),
						                                                            celestialObjectChild.getDisplayName() )
								                               .setStyle(Commons.getStyleWarning()) );
						commandSource.sendFeedback(new TranslationTextComponent("warpdrive.command.specify_explicit_dimension")
								                           .setStyle(Commons.getStyleCorrect()), false);
						continue;
					} else {
						dimensionIdTarget = celestialObjectChild.dimensionId;
						final VectorI vEntry = celestialObjectChild.getEntryOffset();
						xTarget += vEntry.x;
						yTarget += vEntry.y;
						zTarget += vEntry.z;
					}
				} else {
					// on a planet => move to space
					if ( celestialObjectCurrent.parent == null
					  || celestialObjectCurrent.parent.isVirtual() ) {
						WarpDrive.logger.error(String.format("Unable to target a null or virtual parent dimension of %s.", celestialObjectCurrent.dimensionId));
						dimensionIdTarget = null;
						
					} else {
						dimensionIdTarget = celestialObjectCurrent.parent.dimensionId;
						final VectorI vEntry = celestialObjectCurrent.getEntryOffset();
						xTarget -= vEntry.x;
						yTarget -= vEntry.y;
						zTarget -= vEntry.z;
					}
				}
				
			} else {
				// adjust offset when it's directly above or below us
				if ( celestialObjectCurrent != null
				  && celestialObjectCurrent.parent != null
				  && celestialObjectCurrent.parent.dimensionId.equals(dimensionIdTarget) ) {// moving to parent explicitly
					final VectorI vEntry = celestialObjectCurrent.getEntryOffset();
					xTarget -= vEntry.x;
					yTarget -= vEntry.y;
					zTarget -= vEntry.z;
				} else {
					final CelestialObject celestialObjectChild = CelestialObjectManager.getClosestChild(serverPlayerEntity.world, (int) serverPlayerEntity.getPosX(), (int) serverPlayerEntity.getPosZ());
					if ( celestialObjectChild != null
					  && celestialObjectChild.dimensionId.equals(dimensionIdTarget) ) {// moving to child explicitly
						final VectorI vEntry = celestialObjectChild.getEntryOffset();
						xTarget += vEntry.x;
						yTarget += vEntry.y;
						zTarget += vEntry.z;
					}
				}
			}
			
			// validate the target dimension name
			if (dimensionIdTarget == null) {
				commandSource.sendErrorMessage(new TranslationTextComponent("warpdrive.command.undefined_dimension",
				                                                            target).setStyle(Commons.getStyleWarning()) );
				continue;
			}
			DimensionType dimensionTypeTarget = DimensionType.byName(dimensionIdTarget);
			if (dimensionTypeTarget == null) {
				commandSource.sendErrorMessage(new TranslationTextComponent("warpdrive.command.undefined_dimension",
				                                                            dimensionIdTarget).setStyle(Commons.getStyleWarning()) );
				continue;
			}
			
			// get target celestial object
			final CelestialObject celestialObjectTarget = CelestialObjectManager.get(false, dimensionTypeTarget.getRegistryName(), xTarget, zTarget);
			
			// force to center if we're outside the border
			if ( celestialObjectTarget != null
			  && !celestialObjectTarget.isInsideBorder(xTarget, zTarget) ) {
				// outside 
				xTarget = celestialObjectTarget.dimensionCenterX;
				zTarget = celestialObjectTarget.dimensionCenterZ;
			}
			
			// get target world
			final ServerWorld worldTarget = Commons.getOrCreateWorldServer(dimensionTypeTarget);
			if (worldTarget == null) {
				commandSource.sendErrorMessage(new TranslationTextComponent("warpdrive.command.undefined_dimension",
				                                                            Commons.format(dimensionTypeTarget)).setStyle(Commons.getStyleWarning()) );
				continue;
			}
			
			// inform player
			final ITextComponent textComponent = new TranslationTextComponent("warpdrive.command.teleporting_player_x_to_y",
			                                                                  serverPlayerEntity.getName().getFormattedText(),
			                                                                  Commons.format(worldTarget)).setStyle(Commons.getStyleCorrect());
			commandSource.sendFeedback(textComponent, true);
			WarpDrive.logger.info(textComponent.getString());
			if (commandSource.getEntity() != serverPlayerEntity) {
				Commons.addChatMessage(serverPlayerEntity, new TranslationTextComponent("warpdrive.command.teleporting_by_x_to_y",
						commandSource.getName(), Commons.format(worldTarget), dimensionTypeTarget.getRegistryName()).setStyle(Commons.getStyleCorrect()));
			}
			
			// find a good spot
			if ( (worldTarget.isAirBlock(new BlockPos(xTarget, yTarget - 1, zTarget)) && !serverPlayerEntity.abilities.allowFlying)
			  || !worldTarget.isAirBlock(new BlockPos(xTarget, yTarget    , zTarget))
			  || !worldTarget.isAirBlock(new BlockPos(xTarget, yTarget + 1, zTarget)) ) {// non solid ground and can't fly, or inside blocks
				yTarget = worldTarget.getHeight(Type.MOTION_BLOCKING_NO_LEAVES, xTarget, zTarget) + 1;
				if (yTarget == 0) {
					yTarget = 128;
				} else {
					for (int safeY = yTarget - 3; safeY > Math.max(1, yTarget - 20); safeY--) {
						if (!worldTarget.isAirBlock(new BlockPos(xTarget, safeY - 1, zTarget))
						  && worldTarget.isAirBlock(new BlockPos(xTarget, safeY    , zTarget))
						  && worldTarget.isAirBlock(new BlockPos(xTarget, safeY + 1, zTarget))) {
							yTarget = safeY;
							break;
						}
					}
				}
			}
			
			// actual teleportation
			Commons.moveEntity(serverPlayerEntity, worldTarget, new Vector3(xTarget + 0.5D, yTarget + 0.2D, zTarget + 0.5D));
		}
		
		return serverPlayerEntities.size();
	}
}