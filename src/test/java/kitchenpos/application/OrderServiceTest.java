package kitchenpos.application;

import static kitchenpos.domain.OrderStatus.COMPLETION;
import static kitchenpos.fixture.MenuFixture.메뉴;
import static kitchenpos.fixture.MenuGroupFixture.메뉴_그룹;
import static kitchenpos.fixture.MenuProductFixture.메뉴_상품;
import static kitchenpos.fixture.OrderTableFixture.주문_테이블;
import static kitchenpos.fixture.ProductFixture.상품;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import kitchenpos.application.dto.order.OrderCreateRequest;
import kitchenpos.application.dto.order.OrderCreateRequest.OrderLineItemCreateRequest;
import kitchenpos.application.dto.order.OrderCreateResponse;
import kitchenpos.application.dto.order.OrderResponse;
import kitchenpos.application.dto.order.OrderStatusRequest;
import kitchenpos.dao.MenuDao;
import kitchenpos.dao.MenuGroupDao;
import kitchenpos.dao.MenuProductDao;
import kitchenpos.dao.OrderTableDao;
import kitchenpos.dao.ProductDao;
import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuGroup;
import kitchenpos.domain.MenuProduct;
import kitchenpos.domain.Order;
import kitchenpos.domain.OrderLineItem;
import kitchenpos.domain.OrderStatus;
import kitchenpos.domain.OrderTable;
import kitchenpos.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@Sql("classpath:truncate.sql")
@TestConstructor(autowireMode = AutowireMode.ALL)
class OrderServiceTest {

    private final OrderService orderService;
    private final OrderTableDao orderTableDao;
    private final ProductDao productDao;
    private final MenuProductDao menuProductDao;
    private final MenuGroupDao menuGroupDao;
    private final MenuDao menuDao;

    private Menu 저장된_메뉴;
    private OrderTable 저장된_주문_테이블;

    public OrderServiceTest(final OrderService orderService,
                            final OrderTableDao orderTableDao,
                            final ProductDao productDao,
                            final MenuProductDao menuProductDao,
                            final MenuGroupDao menuGroupDao,
                            final MenuDao menuDao) {
        this.orderService = orderService;
        this.orderTableDao = orderTableDao;
        this.productDao = productDao;
        this.menuProductDao = menuProductDao;
        this.menuGroupDao = menuGroupDao;
        this.menuDao = menuDao;
    }

    @BeforeEach
    void setUp() {
        final OrderTable 주문_테이블 = 주문_테이블(null, null, 2, false);
        저장된_주문_테이블 = orderTableDao.save(주문_테이블);

        final MenuGroup 메뉴_그룹 = 메뉴_그룹(null, "양념 반 후라이드 반");
        final MenuGroup 저장된_메뉴_그룹 = menuGroupDao.save(메뉴_그룹);

        final Product 저장된_양념_치킨 = productDao.save(상품(null, "양념 치킨", BigDecimal.valueOf(12000, 2)));
        final Product 저장된_후라이드_치킨 = productDao.save(상품(null, "후라이드 치킨", BigDecimal.valueOf(10000, 2)));
        final MenuProduct 메뉴_상품_1 = 메뉴_상품(null, null, 저장된_양념_치킨.getId(), 1);
        final MenuProduct 메뉴_상품_2 = 메뉴_상품(null, null, 저장된_후라이드_치킨.getId(), 1);

        final Menu 메뉴 = 메뉴(null, "메뉴", BigDecimal.valueOf(22000, 2), 저장된_메뉴_그룹.getId(), List.of(메뉴_상품_1, 메뉴_상품_2));
        저장된_메뉴 = menuDao.save(메뉴);

        메뉴_상품_1.setMenuId(저장된_메뉴.getId());
        메뉴_상품_2.setMenuId(저장된_메뉴.getId());
        menuProductDao.save(메뉴_상품_1);
        menuProductDao.save(메뉴_상품_2);
    }

    @Nested
    class 주문_등록_시 {

        @Test
        void 주문을_정상적으로_등록한다() {
            // given
            final OrderCreateRequest 주문_요청값 = new OrderCreateRequest(저장된_주문_테이블.getId(),
                    List.of(new OrderLineItemCreateRequest(저장된_메뉴.getId(), 1)));

            // when
            final OrderCreateResponse 저장된_주문 = orderService.create(주문_요청값);

            // then
            final OrderCreateResponse 예상_응답값 = OrderCreateResponse.of(
                    new Order(null, 저장된_주문_테이블.getId(), OrderStatus.COOKING.name(), LocalDateTime.now(),
                            List.of(new OrderLineItem(저장된_메뉴.getId(), 1))));

            assertAll(
                    () -> assertThat(저장된_주문.getId()).isNotNull(),
                    () -> assertThat(저장된_주문).usingRecursiveComparison()
                            .ignoringFields("id", "orderedTime", "orderLineItems.seq", "orderLineItems.orderId")
                            .isEqualTo(예상_응답값)
            );
        }

        @Test
        void 주문_메뉴_목록이_비어있으면_예외가_발생한다() {
            // given
            final OrderCreateRequest 주문_요청값 = new OrderCreateRequest(저장된_주문_테이블.getId(),
                    Collections.emptyList());

            // expected
            assertThatThrownBy(() -> orderService.create(주문_요청값))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 주문_메뉴_목록들_중_존재하지_않는_메뉴가_있으면_예외가_발생한다() {
            // given
            final OrderCreateRequest 주문_요청값 = new OrderCreateRequest(저장된_주문_테이블.getId(),
                    List.of(new OrderLineItemCreateRequest(저장된_메뉴.getId() + 1, 1)));

            // expected
            assertThatThrownBy(() -> orderService.create(주문_요청값))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 주문_테이블이_존재하지_않으면_예외가_발생한다() {
            // given
            final OrderCreateRequest 주문_요청값 = new OrderCreateRequest(저장된_주문_테이블.getId() + 1,
                    List.of(new OrderLineItemCreateRequest(저장된_메뉴.getId(), 1)));

            // expected
            assertThatThrownBy(() -> orderService.create(주문_요청값))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 주문_테이블이_빈_테이블이면_예외가_발생한다() {
            // given
            final OrderTable 주문_테이블 = 주문_테이블(null, null, 2, true);
            final OrderTable 저장된_주문_테이블 = orderTableDao.save(주문_테이블);

            final OrderCreateRequest 주문_요청값 = new OrderCreateRequest(저장된_주문_테이블.getId(),
                    List.of(new OrderLineItemCreateRequest(저장된_메뉴.getId(), 1)));

            // expected
            assertThatThrownBy(() -> orderService.create(주문_요청값))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void 주문_목록을_정상적으로_조회한다() {
        // given
        final OrderCreateRequest 주문_요청값 = new OrderCreateRequest(저장된_주문_테이블.getId(),
                List.of(new OrderLineItemCreateRequest(저장된_메뉴.getId(), 1)));

        orderService.create(주문_요청값);

        // when
        final List<OrderResponse> 주문들 = orderService.list();

        // then
        final List<OrderResponse> 예상_응답값 = List.of(
                OrderResponse.of(
                        new Order(null, 저장된_주문_테이블.getId(), OrderStatus.COOKING.name(), LocalDateTime.now(),
                                List.of(new OrderLineItem(저장된_메뉴.getId(), 1))))
        );

        assertThat(주문들).usingRecursiveComparison()
                .ignoringFields("id", "orderedTime", "orderLineItems.seq", "orderLineItems.orderId")
                .isEqualTo(예상_응답값);
    }

    @Nested
    class 주문_상태_변경_시 {

        @Test
        void 주문_상태를_정상적으로_변경한다() {
            // given
            final OrderCreateRequest 주문_요청값 = new OrderCreateRequest(저장된_주문_테이블.getId(),
                    List.of(new OrderLineItemCreateRequest(저장된_메뉴.getId(), 1)));

            final OrderCreateResponse 저장된_주문 = orderService.create(주문_요청값);

            // when
            final OrderResponse 상태가_변경된_주문 = orderService.changeOrderStatus(저장된_주문.getId(),
                    new OrderStatusRequest(COMPLETION));

            // then
            assertThat(상태가_변경된_주문.getOrderStatus()).isEqualTo(COMPLETION.name());
        }

        @Test
        void 주문이_존재하지_않으면_예외가_발생한다() {
            // given
            final OrderCreateRequest 주문_요청값 = new OrderCreateRequest(저장된_주문_테이블.getId(),
                    List.of(new OrderLineItemCreateRequest(저장된_메뉴.getId(), 1)));
            final OrderCreateResponse 저장된_주문 = orderService.create(주문_요청값);

            // expected
            assertThatThrownBy(
                    () -> orderService.changeOrderStatus(저장된_주문.getId() + 1, new OrderStatusRequest(COMPLETION))
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void 주문의_상태가_COMPLETION이면_예외가_발생한다() {
            // given
            final OrderCreateRequest 주문_요청값 = new OrderCreateRequest(저장된_주문_테이블.getId(),
                    List.of(new OrderLineItemCreateRequest(저장된_메뉴.getId(), 1)));
            final OrderCreateResponse 저장된_주문 = orderService.create(주문_요청값);
            orderService.changeOrderStatus(저장된_주문.getId(), new OrderStatusRequest(COMPLETION));

            // expected
            assertThatThrownBy(
                    () -> orderService.changeOrderStatus(저장된_주문.getId(), new OrderStatusRequest(COMPLETION))
            ).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
