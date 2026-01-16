package ru.practicum.client;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

@Component
public class CollectorGrpcClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void sendUserAction(UserActionProto action) {
        client.collectUserAction(action);
    }
}