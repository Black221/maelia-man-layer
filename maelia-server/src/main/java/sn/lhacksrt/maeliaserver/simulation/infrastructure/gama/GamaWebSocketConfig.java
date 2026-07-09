package sn.lhacksrt.maeliaserver.simulation.infrastructure.gama;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Configuration
public class GamaWebSocketConfig {

    @Value("${gama.ws-url:ws://localhost:6868}")
    private String gamaWsUrl;

    @Bean
    public WebSocketClient gamaWebSocketClient() {
        return new StandardWebSocketClient();
    }

    public String getGamaWsUrl() { return gamaWsUrl; }
}
