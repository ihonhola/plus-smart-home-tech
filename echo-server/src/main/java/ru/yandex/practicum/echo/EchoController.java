package ru.yandex.practicum.echo;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.practicum.grpc.echo.EchoRequest;
import ru.yandex.practicum.grpc.echo.EchoResponse;
import ru.yandex.practicum.grpc.echo.EchoServiceGrpc;

@GrpcService
public class EchoController extends EchoServiceGrpc.EchoServiceImplBase {

    /*@Override
    public void echo(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
        EchoResponse response = EchoResponse.newBuilder()
                .setMessage("Server received: " + request.getMessage())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }*/
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void echo(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
        try {
            log.info("echo received");
            responseObserver.onNext(EchoResponse.newBuilder().setMessage(request.getMessage()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}