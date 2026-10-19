package tr.kitaptakip.utils;

import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import tr.kitaptakip.exceptions.BusinessRuleException;

public final class PageRequestFactory {
    private PageRequestFactory() {
    }

    public static Pageable create(int page, int size, String sort, Set<String> allowedProperties,
                                  String defaultSort) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessRuleException("Sayfa sıfır veya daha büyük, sayfa boyutu 1 ile 100 arasında olmalıdır.");
        }
        String[] parts = (sort == null || sort.isBlank() ? defaultSort : sort).split(",", -1);
        if (parts.length > 2 || parts[0].isBlank() || !allowedProperties.contains(parts[0])) {
            throw new BusinessRuleException("Sıralama alanı geçersiz.");
        }
        Sort.Direction direction;
        try {
            direction = parts.length == 1 ? Sort.Direction.ASC : Sort.Direction.fromString(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("Sıralama yönü 'asc' veya 'desc' olmalıdır.");
        }
        return PageRequest.of(page, size, Sort.by(direction, parts[0]));
    }
}
