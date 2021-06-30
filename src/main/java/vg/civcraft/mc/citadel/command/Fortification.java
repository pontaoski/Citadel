package vg.civcraft.mc.citadel.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.CitadelPermissionHandler;
import vg.civcraft.mc.citadel.CitadelUtility;
import vg.civcraft.mc.citadel.playerstate.AbstractPlayerState;
import vg.civcraft.mc.citadel.playerstate.FortificationState;
import vg.civcraft.mc.citadel.playerstate.PlayerStateManager;
import vg.civcraft.mc.citadel.reinforcementtypes.ReinforcementType;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.command.TabCompleters.GroupTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;

@CommandAlias("ctf")
public class Fortification extends BaseCommand {

	@Syntax("/ctf <group>")
	@Description("Enters fortification mode. All blocks placed in fortification mode will automatically be reinforced. If no group is given, your default group will be used")
	public void execute(CommandSender sender, @Optional String targetGroup) {
		Player player = (Player) sender;
		PlayerStateManager stateManager = Citadel.getInstance().getStateManager();
		AbstractPlayerState currentState = stateManager.getState(player);
		if (targetGroup.isEmpty() && currentState instanceof FortificationState) {
			stateManager.setState(player, null);
			return;
		}
		ReinforcementType type = Citadel.getInstance().getReinforcementTypeManager()
				.getByItemStack(player.getInventory().getItemInMainHand());
		if (type == null) {
			CitadelUtility.sendAndLog(player, ChatColor.RED, "You can not reinforce with this item");
			stateManager.setState(player, null);
			return;
		}
		
		String groupName = null;
		if (targetGroup.isEmpty()) {
			groupName = NameAPI.getGroupManager().getDefaultGroup(player.getUniqueId());
			if (groupName == null) {
				CitadelUtility.sendAndLog(player, ChatColor.RED,
						"You need to fortify to a group! Try /fortify groupname. \n Or use /create groupname if you don't have a group yet.");
				return;
			}
		} else {
			groupName = targetGroup;
		}
		
		Group group = GroupManager.getGroup(groupName);
		if (group == null) {
			CitadelUtility.sendAndLog(player, ChatColor.RED, "The group " + groupName + " does not exist.");
			stateManager.setState(player, null);
			return;
		}
		boolean hasAccess = NameAPI.getGroupManager().hasAccess(group.getName(), player.getUniqueId(),
				CitadelPermissionHandler.getReinforce());
		if (!hasAccess) {
			CitadelUtility.sendAndLog(player, ChatColor.RED, "You do not have permission to reinforce on " + group.getName());
			stateManager.setState(player, null);
			return;
		}
		if (currentState instanceof FortificationState) {
			FortificationState fortState = (FortificationState) currentState;
			if (fortState.getGroup() == group && fortState.getType() == type) {
				stateManager.setState(player, null);
				return;
			}
		}
		stateManager.setState(player, new FortificationState(player, type, group));
	}

	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 0)
			return GroupTabCompleter.complete(null, CitadelPermissionHandler.getReinforce(),
					(Player) sender);
		else if (args.length == 1)
			return GroupTabCompleter.complete(args[0], CitadelPermissionHandler.getReinforce(),
					(Player) sender);
		else {
			return new ArrayList<>();
		}
	}
}
