package soon.fridgely.domain.member.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DeviceIdBuffer {

    public static final int CHUNK_SIZE = 1000;

    private final List<Long> ids = new ArrayList<>(CHUNK_SIZE);

    public void add(long deviceId, Consumer<List<Long>> onFlush) {
        ids.add(deviceId);
        if (ids.size() >= CHUNK_SIZE) {
            flush(onFlush);
        }
    }

    public void flush(Consumer<List<Long>> onFlush) {
        if (ids.isEmpty()) {
            return;
        }
        onFlush.accept(new ArrayList<>(ids));
        ids.clear();
    }
}
