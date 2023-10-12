package kitchenpos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import kitchenpos.domain.Product;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@Sql("classpath:truncate.sql")
@TestConstructor(autowireMode = AutowireMode.ALL)
class ProductServiceTest {

    private final ProductService productService;

    public ProductServiceTest(final ProductService productService) {
        this.productService = productService;
    }

    @Test
    void 상품을_정상적으로_등록한다() {
        // given
        final Product 등록할_상품 = new Product("상품", BigDecimal.valueOf(10000, 2));

        // when
        final Product 등록된_상품 = productService.create(등록할_상품);

        // then
        assertThat(등록된_상품).isEqualTo(등록할_상품);
    }

    @Test
    void 상품_등록시_상품의_가격이_0보다_작으면_예외가_발생한다() {
        // given
        final Product 등록할_상품 = new Product("상품", BigDecimal.valueOf(-10000));

        // expected
        assertThatThrownBy(() -> productService.create(등록할_상품))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 상품_등록시_상품의_가격이_없으면_예외가_발생한다() {
        // given
        final Product 등록할_상품 = new Product("상품", null);

        // expected
        assertThatThrownBy(() -> productService.create(등록할_상품))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 상품_목록을_정상적으로_조회한다() {
        // given
        final Product 상품1 = productService.create(new Product("상품1", BigDecimal.valueOf(10000)));
        final Product 상품2 = productService.create(new Product("상품2", BigDecimal.valueOf(20000)));

        // when
        final List<Product> 상품들 = productService.list();

        // then
        assertThat(상품들).isEqualTo(List.of(상품1, 상품2));
    }
}