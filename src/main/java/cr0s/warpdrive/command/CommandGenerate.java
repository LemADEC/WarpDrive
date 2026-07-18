package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.structures.AbstractStructure;
import cr0s.warpdrive.config.structures.StructureManager;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.EnumStructureGroup;
import cr0s.warpdrive.world.WorldGenSmallShip;
import cr0s.warpdrive.world.WorldGenStation;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

public class CommandGenerate {
	
	public static class StructureGroupArgument implements ArgumentType<String> {
		
		@Nonnull
		public static StructureGroupArgument create() {
			return new StructureGroupArgument();
		}
		
		@Nonnull
		public static <S> String get(@Nonnull final CommandContext<S> context, @Nonnull final String name) throws CommandSyntaxException {
			return context.getArgument(name, String.class);
		}
		
		@Nonnull
		@Override
		public String parse(@Nonnull final StringReader reader) throws CommandSyntaxException {
			return reader.readUnquotedString();
		}
		
		@Override
		public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
			return ISuggestionProvider.suggest(getExamples().stream(), builder);
		}
		
		@Override
		public Collection<String> getExamples() {
			final Collection<String> examples = new ArrayList<>(Arrays.asList(StructureManager.getGroups()));
			examples.add("ship");
			examples.add("station");
			return examples;
		}
	}
	
	public static class StructureNameArgument implements ArgumentType<String> {
		
		@Nonnull
		public static StructureNameArgument create() {
			return new StructureNameArgument();
		}
		
		@Nonnull
		public static <S> String get(@Nonnull final CommandContext<S> context, @Nonnull final String name) throws CommandSyntaxException {
			return context.getArgument(name, String.class);
		}
		
		@Nonnull
		@Override
		public String parse(@Nonnull final StringReader reader) throws CommandSyntaxException {
			return reader.readUnquotedString();
		}
		
		@Override
		public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
			String group;
			try {
				group = StructureGroupArgument.get(context, "structureGroup");
			} catch (CommandSyntaxException commandSyntaxException) {
				group = commandSyntaxException.toString();
			}
			return ISuggestionProvider.suggest(Arrays.stream(StructureManager.getStructureNames(group)).map(Object::toString), builder);
		}
		
		@Override
		public Collection<String> getExamples() {
			return Arrays.stream(StructureManager.getStructureNames("moon")).map(Object::toString).collect(Collectors.toList());
		}
	}
	
	public static void register(@Nonnull final CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(
				Commands.literal("wgenerate")
				        .requires(commandSource -> commandSource.hasPermissionLevel(2))
				        .then(Commands.argument("structureGroup", new StructureGroupArgument())
				                      .then(Commands.argument("structureName", new StructureNameArgument())
				                                    .executes((commandContext) -> execute(commandContext.getSource(),
				                                                                          Collections.singleton(commandContext.getSource().asPlayer()),
				                                                                          StructureGroupArgument.get(commandContext, "structureGroup"),
				                                                                          StructureNameArgument.get(commandContext, "structureName") ))
				                           )
				                      .executes((commandContext) -> execute(commandContext.getSource(),
				                                                            Collections.singleton(commandContext.getSource().asPlayer()),
				                                                            StringArgumentType.getString(commandContext, "structureGroup"),
				                                                            "" ))
				             )
				        .then(Commands.argument("theEntities", EntityArgument.entities()) // note: arguments are sorted alphabetically, we want theEntities after structureGroup
				                      .then(Commands.argument("structureGroup", new StructureGroupArgument())
				                                    .then(Commands.argument("structureName", new StructureNameArgument())
				                                                  .executes((commandContext) -> execute(commandContext.getSource(),
				                                                                                        EntityArgument.getEntities(commandContext, "theEntities"),
				                                                                                        StructureGroupArgument.get(commandContext, "structureGroup"),
				                                                                                        StructureNameArgument.get(commandContext, "structureName") ))
				                                         )
				                                    .executes((commandContext) -> execute(commandContext.getSource(),
				                                                                          EntityArgument.getEntities(commandContext, "theEntities"),
				                                                                          StructureGroupArgument.get(commandContext, "structureGroup"),
				                                                                          "" ))
				                           )
				                      .executes((commandContext) -> execute(commandContext.getSource(),
				                                                            EntityArgument.getEntities(commandContext, "theEntities"),
				                                                            "ship",
				                                                            "" ))
				             )
				        .then(Commands.argument("position", BlockPosArgument.blockPos())
				                      .requires(commandSource -> commandSource.getEntity() != null)
				                      .then(Commands.argument("structureGroup", new StructureGroupArgument())
				                                    .then(Commands.argument("structureName", new StructureNameArgument())
				                                                  .executes((commandContext) -> execute(commandContext.getSource(),
				                                                                                        commandContext.getSource().asPlayer().getEntityWorld(),
				                                                                                        BlockPosArgument.getBlockPos(commandContext, "position"),
				                                                                                        StructureGroupArgument.get(commandContext, "structureGroup"),
				                                                                                        StructureNameArgument.get(commandContext, "structureName") ))
				                                         )
				                                    .executes((commandContext) -> execute(commandContext.getSource(),
				                                                                          commandContext.getSource().asPlayer().getEntityWorld(),
				                                                                          BlockPosArgument.getBlockPos(commandContext, "position"),
				                                                                          StructureGroupArgument.get(commandContext, "structureGroup"),
				                                                                          "" ))
				                           )
				                      .executes((commandContext) -> execute(commandContext.getSource(),
				                                                            commandContext.getSource().asPlayer().getEntityWorld(),
				                                                            BlockPosArgument.getBlockPos(commandContext, "position"),
				                                                            "ship",
				                                                            "" ))
				             )
				        
				        .then(Commands.literal("help")
				                      .executes((commandContext) -> help(commandContext.getSource(),
				                                                         commandContext.getNodes().get(0).getNode().getName() ))
				             )
				        .executes((commandContext) -> execute(commandContext.getSource(),
				                                              Collections.singleton(commandContext.getSource().asPlayer()),
				                                              "ship",
				                                              "" ))
		                   );
	}
	
	private static int help(@Nonnull final CommandSource commandSource, @Nonnull final String name) {
		commandSource.sendFeedback(new StringTextComponent(String.format( "/%s <structure group> (<structure name>)\n"
		                                                                + "Structure groups are ship, station, astfield, %s",
		                                                                 name, Commons.format(StructureManager.getGroups()).replace("\"", "") )), false);
		return 0;
	}
	
	private static int execute(@Nonnull final CommandSource commandSource,
	                           @Nonnull final Collection<? extends Entity> entities,
	                           @Nonnull final String structureGroup,
	                           @Nonnull final String structureName) {
		for (final Entity entity : entities) {
			final World world = entity.getEntityWorld();
			BlockPos blockPos = entity.getPosition();
			
			execute(commandSource, world, blockPos, structureGroup, structureName);
		}
		return entities.size();
	}
	
	private static int execute(@Nonnull final CommandSource commandSource,
	                           @Nonnull final World world,
	                           @Nonnull final BlockPos blockPos,
	                           @Nonnull final String structureGroup,
	                           @Nonnull final String structureName) {
		
		// Reject command, if target is not in space
		if (!CelestialObjectManager.isInSpace(world, blockPos.getX(), blockPos.getZ()) && (!"ship".equals(structureGroup))) {
			commandSource.sendErrorMessage(new TranslationTextComponent("warpdrive.command.only_in_space").setStyle(Commons.getStyleWarning()));
			return 0;
		}
		
		switch (structureGroup) {
		case "ship":
			WarpDrive.logger.info(String.format("/generate: generating NPC ship %s",
			                                    Commons.format(world, blockPos)));
			new WorldGenSmallShip(false, true).place(world, world.rand, blockPos);
			break;
		
		case "station":
			WarpDrive.logger.info(String.format("/generate: generating station %s",
			                                    Commons.format(world, blockPos)));
			new WorldGenStation(false).place(world, world.rand, blockPos);
			break;
		
		case "astfield":
			generateStructure(commandSource, EnumStructureGroup.ASTEROIDS_FIELDS.getName(), structureName, world, blockPos);
			break;
		
		case "gascloud":
			generateStructure(commandSource, EnumStructureGroup.GAS_CLOUDS.getName(), structureName, world, blockPos);
			break;
		
		default:
			generateStructure(commandSource, structureGroup, structureName, world, blockPos);
			break;
		}
		return 1;
	}
	
	private static void generateStructure(@Nonnull final CommandSource commandSource,
	                                      @Nonnull final String group, @Nonnull final String name,
	                                      @Nonnull final World world, @Nonnull final BlockPos blockPos) {
		final AbstractStructure structure = StructureManager.getStructure(world.rand, group, name);
		if (structure == null) {
			if (Arrays.asList(StructureManager.getGroups()).contains(group)) {
				commandSource.sendErrorMessage(new TranslationTextComponent("Invalid structure name %1$s:%2$s.\nTry one of the followings: %3$s.",
				                                                            group, name, String.join(", ", StructureManager.getStructureNames(group)) )
						                               .setStyle(Commons.getStyleWarning()) );
			} else {
				commandSource.sendErrorMessage(new TranslationTextComponent("Invalid structure group %1$s.\nTry one of the followings: %2$s.",
				                                                            group, String.join(", ", StructureManager.getGroups()) )
						                               .setStyle(Commons.getStyleWarning()) );
			}
		} else {
			WarpDrive.logger.info(String.format("Generating %s:%s %s",
			                                    group, structure.getName(), Commons.format(world, blockPos)));
			structure.place(world, world.rand, blockPos);
			
			// do a weak attempt to extract player (ideally, it should be delayed after generation, but that's too complicated)
			if (commandSource.getEntity() instanceof ServerPlayerEntity) {
				int newY = blockPos.getY() + 1;
				while ( newY < 256
				     && !world.isAirBlock(new BlockPos(blockPos.getX(), newY, blockPos.getZ())) ) {
					newY++;
				}
				final ServerPlayerEntity entityServerPlayer = (ServerPlayerEntity) commandSource.getEntity();
				entityServerPlayer.setPosition(entityServerPlayer.getPosX(), newY, entityServerPlayer.getPosZ());
			}
		}
	}
}