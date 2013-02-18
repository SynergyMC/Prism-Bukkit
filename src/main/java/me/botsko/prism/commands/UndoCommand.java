package me.botsko.prism.commands;

import java.util.List;

import org.bukkit.ChatColor;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionMessage;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.actions.Action;
import me.botsko.prism.actions.ActionType;
import me.botsko.prism.actions.PrismProcessAction;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.appliers.Undo;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.SubHandler;
import me.botsko.prism.utils.TypeUtils;

public class UndoCommand implements SubHandler {
	
	/**
	 * 
	 */
	private Prism plugin;
	
	
	/**
	 * 
	 * @param plugin
	 * @return 
	 */
	public UndoCommand(Prism plugin) {
		this.plugin = plugin;
	}
	
	
	/**
	 * Handle the command
	 */
	public void handle(CallInfo call) {
		
		if( call.getArgs().length > 0){
			
			ActionsQuery aq = new ActionsQuery(plugin);
			
			int record_id = 0;
			if(TypeUtils.isNumeric(call.getArg(0))){
				record_id = Integer.parseInt(call.getArg(0));
				if(record_id <= 0){
					call.getPlayer().sendMessage( plugin.messenger.playerError("Record id must be greater than zero." ) );
					return;
				}
			} else {
				if(call.getArg(0).equals("last")){
					record_id = aq.getUsersLastPrismProcessId( call.getPlayer().getName() );
				}
			}
			
			// Invalid id
			if(record_id == 0){
				call.getPlayer().sendMessage( plugin.messenger.playerError("Either you have no last process or an invalid id." ) );
				return;
			}
			
			
			PrismProcessAction process = aq.getPrismProcessRecord( record_id );
			if(process == null){
				call.getPlayer().sendMessage( plugin.messenger.playerError("A process does not exists with that value." ) );
				return;
			}

			// We only support this for drains
			if(!process.getProcessChildActionType().equals(ActionType.PRISM_DRAIN)){
				call.getPlayer().sendMessage( plugin.messenger.playerError("You can't currently undo anything other than a drain process." ) );
				return;
			}
			
			// Pull the actual block change data for this undo event
			QueryParameters parameters = new QueryParameters();
			parameters.setWorld(call.getPlayer().getWorld().getName());
			parameters.addActionType(process.getProcessChildActionType());
			parameters.addPlayerName( call.getPlayer().getName() );
			parameters.setParentId(record_id);
			
			// make sure the distance isn't too far away
			
			QueryResult results = aq.lookup( parameters, call.getPlayer() );
			if(!results.getActionResults().isEmpty()){
				
				call.getPlayer().sendMessage( plugin.messenger.playerHeaderMsg("Undoing..." + ChatColor.GRAY + " Abandon ship!") );
				
				Undo rb = new Undo( plugin, call.getPlayer(), PrismProcessType.UNDO, results.getActionResults(), parameters );
				rb.apply();
				
			} else {
				call.getPlayer().sendMessage( plugin.messenger.playerError( "Nothing found to undo. Must be a problem with Prism." ) );
			}
			
			
		} else {
			
			// Show the list
			// Process and validate all of the arguments
			QueryParameters parameters = new QueryParameters();
			parameters.setAllowNoRadius(true);
			parameters.addActionType(ActionType.PRISM_PROCESS);
			parameters.addPlayerName( call.getPlayer().getName() );
			parameters.setLimit(5); // @todo config this, and move the logic to queryparams
		
			ActionsQuery aq = new ActionsQuery(plugin);
			QueryResult results = aq.lookup( parameters, call.getPlayer() );
			if(!results.getActionResults().isEmpty()){
				call.getPlayer().sendMessage( plugin.messenger.playerHeaderMsg("Showing "+results.getTotal_results()+" results. Page 1 of "+results.getTotal_pages()) );
				call.getPlayer().sendMessage( plugin.messenger.playerSubduedHeaderMsg("Use /prism undo [id] to reverse a process") );
				List<Action> paginated = results.getPaginatedActionResults();
				if(paginated != null){
					for(Action a : paginated){
						ActionMessage am = new ActionMessage(a);
						if(parameters.allowsNoRadius()){
							am.hideId(false);
						}
						call.getPlayer().sendMessage( plugin.messenger.playerMsg( am.getMessage() ) );
					}
				} else {
					call.getPlayer().sendMessage( plugin.messenger.playerError("Pagination can't find anything. Do you have the right page number?" ) );
				}
			} else {
				call.getPlayer().sendMessage( plugin.messenger.playerError("Nothing found." + ChatColor.GRAY + " Either you're missing something, or we are." ) );
			}
		}
	}
}