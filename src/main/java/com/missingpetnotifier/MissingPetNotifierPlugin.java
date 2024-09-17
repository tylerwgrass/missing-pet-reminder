package com.missingpetnotifier;

import static com.missingpetnotifier.PetHandler.itemMatchesPet;
import com.google.inject.Provides;
import java.util.Arrays;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Missing Pet Notifier"
)
public class MissingPetNotifierPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private MissingPetNotifierConfig config;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ItemManager itemManager;

	private MissingPetNotifierOverlay overlay = new MissingPetNotifierOverlay();
	@Getter
	private boolean shouldSeePet = false;
	@Getter
	private int numMissingTicks = 0;
	private String followerName;

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlay = null;
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (event.getNpc() == client.getFollower())
		{
			followerName = client.getFollower().getName();
			shouldSeePet = true;
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() == client.getLocalPlayer())
		{
			shouldSeePet = false;
			followerName = null;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INVENTORY.getId() || followerName == null)
		{
			return;
		}
		Item[] inventoryItems = event.getItemContainer().getItems();
		final boolean didPickUpPet = Arrays.stream(inventoryItems)
			.anyMatch(item -> itemMatchesPet(client.getItemDefinition(item.getId()).getName(), followerName));
		shouldSeePet = !didPickUpPet;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (shouldSeePet && client.getFollower() == null)
		{
			if (numMissingTicks >= config.timeMissingDelay())
			{
				overlayManager.add(overlay);
			}
			else
			{
				numMissingTicks++;
			}
		}
		else
		{
			numMissingTicks = 0;
			overlayManager.remove(overlay);
		}
	}

	@Provides
	MissingPetNotifierConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MissingPetNotifierConfig.class);
	}
}
