package guru.nicks.commons.encoder;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Delegates all {@link Encoder} methods to the object passed to constructor.
 *
 * @param <T> input value type
 */
@RequiredArgsConstructor
public class DelegatingEncoder<T> implements Encoder<T> {

    @NonNull // Lombok creates runtime nullness check for this own annotation only
    private final Encoder<T> delegate;

    @Override
    public String encode(T value) {
        return delegate.encode(value);
    }

    @Override
    public T decode(String value) {
        return delegate.decode(value);
    }

    @Override
    public boolean retainsSortOrderAfterEncoding() {
        return delegate.retainsSortOrderAfterEncoding();
    }

}
