package sn.lhacksrt.maeliaserver.simulation.infrastructure.gama;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import sn.lhacksrt.maeliaserver.simulation.infrastructure.gama.message.GamaMessageParser;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Résilience du worker face à un crash brutal du serveur GAMA : la fermeture inattendue de la
 * WebSocket doit débloquer {@code waitForEnd} immédiatement (au lieu d'attendre le timeout, qui
 * immobiliserait un consommateur RabbitMQ jusqu'à 30 min).
 */
class GamaServerSessionResilienceTest {

    @Test
    void connectionClosedAbortsWaitForEnd() throws Exception {
        GamaWebSocketHandler handler = new GamaWebSocketHandler();
        GamaServerSession session = new GamaServerSession(handler, new GamaMessageParser());

        // Simule un crash GAMA (fermeture anormale 1006) pendant que le worker attend la fin.
        Thread crasher = new Thread(() -> {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            handler.afterConnectionClosed(null, new CloseStatus(1006, "abnormal closure"));
        });
        crasher.start();

        long t0 = System.currentTimeMillis();
        // Timeout « logique » de 30 s : le test doit finir bien avant, via la détection de fermeture.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> session.waitForEnd(30));
        long elapsedMs = System.currentTimeMillis() - t0;

        assertTrue(elapsedMs < 5_000, "waitForEnd doit s'arrêter à la fermeture, pas au timeout (" + elapsedMs + " ms)");
        assertTrue(ex.getMessage().toLowerCase().contains("plant") || ex.getMessage().contains("fermée"),
                "message d'échec explicite : " + ex.getMessage());
    }

    @Test
    void deliberateCloseIsNotTreatedAsCrash() throws Exception {
        GamaWebSocketHandler handler = new GamaWebSocketHandler();
        GamaServerSession session = new GamaServerSession(handler, new GamaMessageParser());

        session.close(); // fermeture VOLONTAIRE (fin de run)
        // La fermeture WS qui en découle ne doit pas être requalifiée en crash.
        handler.afterConnectionClosed(null, new CloseStatus(1000, "normal"));

        // Comme aucun crash n'a été enregistré, waitForEnd part au timeout (court) et signale
        // un timeout — PAS le message de plantage.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> session.waitForEnd(1));
        assertTrue(ex.getMessage().contains("timed out"),
                "une fermeture volontaire ne doit pas être un crash : " + ex.getMessage());
    }
}
