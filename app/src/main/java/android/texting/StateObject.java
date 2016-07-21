package android.texting;


import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import shared.logic.Member;
import shared.logic.Narrator;
import shared.logic.Player;
import shared.logic.PlayerList;
import shared.logic.Team;
import shared.logic.support.Constants;
import shared.logic.support.Faction;
import shared.logic.support.FactionManager;
import shared.logic.support.RoleTemplate;
import shared.logic.support.rules.Rule;
import shared.logic.support.rules.RuleBool;
import shared.logic.support.rules.RuleInt;
import shared.logic.support.rules.Rules;

public abstract class StateObject {

	public static final String RULES = "Rules";
	public static final String ROLESLIST = "RolesList";
	public static final String PLAYERLISTS = "PlayerLists";
	public static final String DAYLABEL = "DayLabel";
	public static final String GRAVEYARD = "Graveyard";
	public static final String ROLEINFO = "RoleInfo";
	
	
	private Narrator n;
	private FactionManager fManager; 
	private HashMap<String, Object> extraKeys;
	public StateObject(Narrator n, FactionManager fManager){
		states = new ArrayList<String>();
		this.n = n;
		this.fManager = fManager;
		extraKeys = new HashMap<>();
	}
	
	ArrayList<String> states;
	public StateObject addState(String state){
		states.add(state);
		return this;
	}
	
	private void addJRolesList(JSONObject state) throws JSONException{
		JSONArray roles = new JSONArray();
		JSONObject role;
		for(RoleTemplate r: n.getAllRoles()){
			role = new JSONObject();
			role.put(StateObject.roleType, r.getName());
			
			role.put(StateObject.color, r.getColor());
			roles.put(role);
		}
		state.getJSONArray(StateObject.type).put(StateObject.roles);
		state.put(StateObject.roles, roles);
	}
	
	private void addJDayLabel(JSONObject state) throws JSONException{
		String dayLabel;
		if (!n.isStarted()){
			dayLabel = "Night 0";
		}else if(n.isDay()){
			dayLabel = "Day " + n.getDayNumber();
		}else{
			dayLabel = "Night " + n.getDayNumber();
		}
		state.getJSONArray(StateObject.type).put(StateObject.dayLabel);
		state.put(StateObject.dayLabel, dayLabel);
	}
	
	private ArrayList<Team> shouldShowTeam(Player p){
		ArrayList<Team> teams = new ArrayList<>();
		for(Team t: p.getTeams()){
			if(!t.knowsTeam())
				continue;
			if(t.getMembers().remove(p).getLivePlayers().isEmpty())
				continue;
			teams.add(t);
		}
		return teams;			
	}
	
	private void addJRoleInfo(Player p, JSONObject state) throws JSONException{
		JSONObject roleInfo = new JSONObject();
		roleInfo.put(StateObject.roleColor, p.getTeam().getColor());
		roleInfo.put(StateObject.roleName, p.getRoleName());
		roleInfo.put(StateObject.roleDescription, p.getRoleInfo());
		
		ArrayList<Team> knownTeams = shouldShowTeam(p);
		boolean displayTeam = !knownTeams.isEmpty();
		roleInfo.put(StateObject.roleKnowsTeam, displayTeam);
		if(displayTeam){
			JSONArray allyList = new JSONArray();
			JSONObject allyObject;
			for(Team group: knownTeams){
				for(Player ally: group.getMembers().remove(p).getLivePlayers()){
					allyObject = new JSONObject();
					allyObject.put(StateObject.teamAllyName, ally.getName());
					allyObject.put(StateObject.teamAllyRole, ally.getRoleName());
					allyObject.put(StateObject.teamAllyColor, group.getColor());
					allyList.put(allyObject);
				}
				
			}
			roleInfo.put(StateObject.roleTeam, allyList);
		}

		state.getJSONArray(StateObject.type).put(StateObject.roleInfo);
		state.put(StateObject.roleInfo, roleInfo);
	}
	
	private void addJGraveYard(JSONObject state) throws JSONException{
		JSONArray graveYard = new JSONArray();
		
		JSONObject graveMarker;
		String color;
		for(Player p: n.getDeadPlayers().sortByDeath()){
			graveMarker = new JSONObject();
			if(p.isCleaned())
				color = "#FFFFFF";
			else
				color = p.getTeam().getColor();
			graveMarker.put(StateObject.color, color);
			graveMarker.put(StateObject.roleName, p.getDescription());
			graveMarker.put("name", p.getName());
			graveYard.put(graveMarker);
		}
		
		state.getJSONArray(StateObject.type).put(StateObject.graveYard);
		state.put(StateObject.graveYard, graveYard);
	}
	
	private void addJRules(JSONObject state) throws JSONException{
		addJFactions(state);
		JSONObject jRules = new JSONObject();
		Rules rules = n.getRules();
		Rule r;
		JSONObject ruleObject;
		for(String key: rules.rules.keySet()){
			ruleObject = new JSONObject();
			r = rules.getRule(key);
			ruleObject.put("id", r.id);
			ruleObject.put("name", r.name);
			if(r.getClass() == RuleInt.class){
				ruleObject.put("val", ((RuleInt) r).val);
				ruleObject.put("isNum", true);
			}else{
				ruleObject.put("val", ((RuleBool) r).val);
				ruleObject.put("isNum", false);
			}
			jRules.put(r.id, ruleObject);
		}
		String id;
		for(Team t: n.getAllTeams()){
			if(t.getColor().equals(Constants.A_SKIP))
				continue;
			
			id = t.getColor() + "kill";
			ruleObject = new JSONObject();
			ruleObject.put("name", "Has Faction kill");
			ruleObject.put("id", id);
			ruleObject.put("isNum", false);
			ruleObject.put("val", t.canKill());
			jRules.put(id, ruleObject);
			
			id = t.getColor() + "identity";
			ruleObject = new JSONObject();
			ruleObject.put("name", "Knows who allies are");
			ruleObject.put("id", id);
			ruleObject.put("isNum", false);
			ruleObject.put("val", t.knowsTeam());
			jRules.put(id, ruleObject);
			
			id = t.getColor() + "liveToWin";
			ruleObject = new JSONObject();
			ruleObject.put("name", "Must be alive to win");
			ruleObject.put("id", id);
			ruleObject.put("isNum", false);
			ruleObject.put("val", t.getAliveToWin());
			jRules.put(id, ruleObject);
			
			id = t.getColor() + "priority";
			ruleObject = new JSONObject();
			ruleObject.put("name", "Win priority");
			ruleObject.put("id", id);
			ruleObject.put("val", t.getPriority());
			ruleObject.put("isNum", true);
			jRules.put(id, ruleObject);
		}
		
		
		state.getJSONArray(StateObject.type).put(StateObject.rules);
		state.put(StateObject.rules, jRules);
	}
	

	/* all factions object
	 * 
	 * (faction name)  -> faction object
	 * (faction color) -> faction object
	 * factionName     -> list of faction names
	 * 
	 */
	
	/* single faction object
	 * 
	 * name        -> faction name
	 * color       -> faction color
	 * description -> faction description
	 * isEditable  -> can editFaction
	 */
	
	private void addJFactions(JSONObject state) throws JSONException{
		JSONArray fMembers, blacklisted, allies, enemies, factionNames = new JSONArray();
		JSONObject jFaction, jRT, allyInfo, jFactions = new JSONObject();
		for(Faction f: fManager.factions){
			jFaction = new JSONObject();
			fMembers = new JSONArray();
			blacklisted = new JSONArray();
			
			jFaction.put("name", f.getName());
			factionNames.put(f.getName());
			jFaction.put("color", f.getColor());
			jFaction.put("description", f.getDescription());
			jFaction.put("isEditable", f.isEditable);
			
			for(RoleTemplate rt: f.members){
				jRT = new JSONObject();
				jRT.put("name", rt.getName());
				jRT.put("description", rt.getDescription());
				jRT.put("color", rt.getColor());
				jRT.put("rules", new JSONArray(rt.getRules()));
				jFactions.put(rt.getName() + rt.getColor(), jRT);
				fMembers.put(jRT);
			}
			jFaction.put("members", fMembers);
			for(Member rt: f.unavailableRoles){
				jRT = new JSONObject();
				jRT.put("name", rt.getName());
				jRT.put("simpleName", rt.getSimpleName());
				blacklisted.put(jRT);
			}
			jFaction.put("blacklisted", blacklisted);
			
			
			
			
			if(f.isEditable)
				jFaction.put("rules", f.getRules());
			jFactions.put(f.getName(), jFaction);
			jFactions.put(f.getColor(), jFaction);
			
			Team fTeam = f.getTeam();
			if(fTeam == null)
				continue;

			allies = new JSONArray();
			enemies = new JSONArray();
			for(Team t: n.getAllTeams()){
				if(t.getName().equals(Constants.A_SKIP))
					continue;
				if(t == fTeam)
					continue;
				allyInfo = new JSONObject();
				allyInfo.put("color", t.getColor());
				allyInfo.put("name", t.getName());
				if(t.isEnemy(fTeam))
					enemies.put(allyInfo);
				else
					allies.put(allyInfo);
			}
			jFaction.put("allies", allies);
			jFaction.put("enemies", enemies);
			
		}
		jFactions.put(StateObject.factionNames, factionNames);
		
		state.getJSONArray(StateObject.type).put(StateObject.factions);
		state.put(StateObject.factions, jFactions);
	}
	
	private JSONArray getJPlayerArray(PlayerList input, PlayerList selected) throws JSONException{
		JSONArray arr = new JSONArray();
		if(input.isEmpty())
			return arr;
		PlayerList allPlayers = n.getAllPlayers();
		
		JSONObject jo;
		for(Player pi: input){
			jo = new JSONObject();
			jo.put(StateObject.playerName, pi.getName());
			jo.put(StateObject.playerIndex, allPlayers.indexOf(pi) + 1);
			jo.put(StateObject.playerSelected, selected.contains(pi));
			jo.put(StateObject.playerActive, isActive(pi));
			if(n.isStarted() && pi.getVoters() != null){
				jo.put(StateObject.playerVote, pi.getVoters().size());
			}
			arr.put(jo);
		}
			
		
		
		return arr;
	}
	public abstract boolean isActive(Player p);
	
	private JSONArray getJPlayerArray(PlayerList input) throws JSONException{
		return getJPlayerArray(input, new PlayerList());
	}
	private JSONArray getJPlayerArray(PlayerList input, Player p) throws JSONException{
		PlayerList list = new PlayerList();
		if(p != null)
			list.add(p);
		return getJPlayerArray(input, list);
	}
	private void addJPlayerLists(JSONObject state, Player p) throws JSONException{
		JSONObject playerLists = new JSONObject();
		playerLists.put(StateObject.type, new JSONArray());
		
		if(n.isStarted()){
			if(n.isDay){
				PlayerList votes;
				if(n.isInProgress())
					votes = n.getLivePlayers().remove(p);
				else
					votes = n.getAllPlayers().remove(p);
				if(p.isDead())
					votes.clear();
				JSONArray names = getJPlayerArray(votes, p.getVoteTarget());
				playerLists.put("Vote", names);
				playerLists.getJSONArray(StateObject.type).put("Vote");
			}else{
				if(n.isInProgress()){
					String[] abilities = p.getAbilities();
					for(String s_ability: abilities){
						int ability = p.parseAbility(s_ability);
						PlayerList acceptableTargets = new PlayerList();
						for(Player potentialTarget: n.getAllPlayers()){
							if(p.isAcceptableTarget(potentialTarget, ability)){
								acceptableTargets.add(potentialTarget);
							}
						}
						if(acceptableTargets.isEmpty())
							continue;
	
						JSONArray names = getJPlayerArray(acceptableTargets, p.getTargets(ability));
						playerLists.put(s_ability, names);
						playerLists.getJSONArray(StateObject.type).put(s_ability);
					}
					if(playerLists.getJSONArray(StateObject.type).length() == 0){
						JSONArray names = getJPlayerArray(new PlayerList());
						playerLists.put("You have no acceptable night actions tonight!", names);
						playerLists.getJSONArray(StateObject.type).put("You have no acceptable night actions tonight!");
					}
				}else{
					playerLists.put("Game Over", getJPlayerArray(n.getAllPlayers(), new PlayerList()));
					playerLists.getJSONArray(StateObject.type).put("Game Over");
				}
			}
		}else{
			JSONArray names = getJPlayerArray(n.getAllPlayers());
			playerLists.put("Lobby", names);
			playerLists.getJSONArray(StateObject.type).put("Lobby");
		}
		
		
		state.getJSONArray(StateObject.type).put(StateObject.playerLists);
		state.put(StateObject.playerLists, playerLists);
	}

	public abstract JSONObject getObject() throws JSONException;
	public abstract void write(Player p, JSONObject jo) throws JSONException;
	
	public JSONObject send(Player p) throws JSONException{
		JSONObject obj = getObject();
		for(String state: states){
			if(state.equals(RULES))
				addJRules(obj);
			else if(state.equals(ROLESLIST))
				addJRolesList(obj);
			else if(state.equals(PLAYERLISTS))
				addJPlayerLists(obj, p);
			else if(state.equals(DAYLABEL))
				addJDayLabel(obj);
			else if(state.equals(GRAVEYARD))
				addJGraveYard(obj);
			else if(state.equals(ROLEINFO))
				addJRoleInfo(p, obj);
			
		}
		for(String key: extraKeys.keySet()){
			obj.put(key, extraKeys.get(key));
		}
		
		write(p, obj);
		return obj;
	}
	
	public void send(PlayerList players) throws JSONException {
		for(Player p: players){
			send(p);
		}
	}

	public void addKey(String key, Object val) {
		extraKeys.put(key, val);
		
	}
	
	
	
	public static final String guiUpdate = "guiUpdate";

	public static final String dayLabel = "dayLabel";
	
	public static final String type = "type";
	
	public static final String playerLists = "playerLists";

	public static final String requestGameState = "requestGameState";
	public static final String requestChat = "requestChat";

	public static final String roles = "roles";
	public static final String roleType = "roleType";
	public static final String color = "color";

	public static final String roleInfo = "roleInfo";
	public static final String roleColor = "roleColor";
	public static final String roleName = "roleName";
	public static final String roleTeam = "roleTeam";
	public static final String roleDescription = "roleDescription";
	public static final String roleKnowsTeam = "roleKnowsTeam";
	
	public static final String teamName = "teamName";
	public static final String teamAllyColor = "teamAllyColor";
	public static final String teamMembers = "teamMembers";
	public static final String teamAllyName = "teamAllyName";
	public static final String teamAllyRole = "teamAllyRole";

	public static final String gameStart = "gameStart";

	public static final String isDay = "isDay";

	public static final String showButton = "showButton";

	public static final String endedNight = "endedNight";
	
	public static final String graveYard = "graveYard";
	
	public static final String isHost = "isHost";
	public static final String isFinished = "isFinished";
	public static final String addRole = "addRole";
	public static final String removeRole = "removeRole";
	public static final String startGame = "startGame";
	public static final String host = "host";
	public static final String timer = "timer";
	public static final String rules = "rules";
	public static final String ruleChange = "ruleChange";

	public static final String playerName = "playerName";
	public static final String playerIndex = "playerIndex";
	public static final String playerVote = "playerVote";
	public static final String skipVote = "skipVote";
	public static final String playerSelected = "playerSelected";
	public static final String isSkipping = "isSkipping";
	public static final String playerActive = "playerActive";

	public static final String factions = "factions";
	public static final String factionNames = "factionNames";
}