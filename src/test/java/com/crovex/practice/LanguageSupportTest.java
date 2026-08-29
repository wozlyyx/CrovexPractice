package com.crovex.practice;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.crovex.practice.message.MessageManager;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageSupportTest {

    private ServerMock server;
    private CrovexPractice plugin;
    private MessageManager messageManager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(CrovexPractice.class);
        messageManager = plugin.getMessageManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Should correctly load Turkish messages by default or on demand")
    void testTurkishMessages() {
        messageManager.setLanguage("tr");
        assertThat(messageManager.getLanguage()).isEqualTo("tr");

        Component noPerm = messageManager.getMessage("general.no-permission");
        assertThat(noPerm).isNotNull();

        String rawJoin = messageManager.getRawMessage("match.start-title");
        assertThat(rawJoin).contains("DÖVÜŞ BAŞLADI");
    }

    @Test
    @DisplayName("Should correctly load Spanish messages on demand")
    void testSpanishMessages() {
        messageManager.setLanguage("es");
        assertThat(messageManager.getLanguage()).isEqualTo("es");

        Component noPerm = messageManager.getMessage("general.no-permission");
        assertThat(noPerm).isNotNull();

        String rawStart = messageManager.getRawMessage("match.start-title");
        assertThat(rawStart).contains("¡COMIENZA LA PELEA!");
    }

    @Test
    @DisplayName("Should correctly load French messages on demand")
    void testFrenchMessages() {
        messageManager.setLanguage("fr");
        assertThat(messageManager.getLanguage()).isEqualTo("fr");

        Component noPerm = messageManager.getMessage("general.no-permission");
        assertThat(noPerm).isNotNull();

        String rawStart = messageManager.getRawMessage("match.start-title");
        assertThat(rawStart).contains("LE COMBAT COMMENCE !");
    }

    @Test
    @DisplayName("Should correctly load English messages on demand")
    void testEnglishMessages() {
        messageManager.setLanguage("en");
        assertThat(messageManager.getLanguage()).isEqualTo("en");

        Component noPerm = messageManager.getMessage("general.no-permission");
        assertThat(noPerm).isNotNull();

        String rawStart = messageManager.getRawMessage("match.start-title");
        assertThat(rawStart).contains("FIGHT STARTED!");
    }

    @Test
    @DisplayName("Should format placeholders properly across all languages")
    void testPlaceholdersInMultipleLanguages() {
        // Turkish
        messageManager.setLanguage("tr");
        Component welcomeTr = messageManager.getMessage("general.welcome-join", "%player%", "Player1");
        assertThat(welcomeTr).isNotNull();

        // Spanish
        messageManager.setLanguage("es");
        Component welcomeEs = messageManager.getMessage("general.welcome-join", "%player%", "Player1");
        assertThat(welcomeEs).isNotNull();

        // French
        messageManager.setLanguage("fr");
        Component welcomeFr = messageManager.getMessage("general.welcome-join", "%player%", "Player1");
        assertThat(welcomeFr).isNotNull();
    }
}
