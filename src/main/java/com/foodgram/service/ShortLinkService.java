package com.foodgram.service;

import com.foodgram.config.AppProperties;
import java.util.Optional;
import org.hashids.Hashids;
import org.springframework.stereotype.Service;

/** Кодирование id рецепта в короткий хеш (совместимо с python-hashids, salt/min_length как в Django-версии). */
@Service
public class ShortLinkService {

    private final Hashids hashids;

    public ShortLinkService(AppProperties props) {
        this.hashids = new Hashids(props.hashidsSalt(), 4);
    }

    public String encode(long recipeId) {
        return hashids.encode(recipeId);
    }

    public Optional<Long> decode(String hash) {
        try {
            long[] decoded = hashids.decode(hash);
            if (decoded == null || decoded.length == 0) {
                return Optional.empty();
            }
            return Optional.of(decoded[0]);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
