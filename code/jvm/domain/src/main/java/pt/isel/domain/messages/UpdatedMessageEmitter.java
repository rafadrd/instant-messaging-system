package pt.isel.domain.messages;

import java.util.function.Consumer;

public interface UpdatedMessageEmitter {
    void emit(UpdatedMessage signal);

    void onCompletion(Runnable callback);

    void onError(Consumer<Throwable> callback);

    void complete();
}