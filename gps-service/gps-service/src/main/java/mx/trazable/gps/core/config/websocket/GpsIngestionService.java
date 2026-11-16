package mx.trazable.gps.core.config.websocket;


import mx.trazable.gps.core.config.websocket.records.GpsIdentity;
import mx.trazable.gps.core.config.websocket.records.GpsQueuedPulse;
import mx.trazable.gps.model.GpsPulseDTO;
import org.springframework.stereotype.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Service
public class GpsIngestionService {

    private final BlockingQueue<GpsQueuedPulse> queue = new ArrayBlockingQueue<>(20_000);

    public boolean enqueue(GpsIdentity identity, GpsPulseDTO pulse) {

        return queue.offer(new GpsQueuedPulse(identity, pulse));
    }

    public BlockingQueue<GpsQueuedPulse> getQueue() {
        return queue;
    }

    public int getQueueSize() {
        return queue.size();
    }
}