package kitchenpos.order;

import static kitchenpos.menu.domain.MenuFixture.메뉴;
import static kitchenpos.menu.domain.MenuProductFixture.메뉴_상품;
import static kitchenpos.menugroup.domain.MenuGroupFixture.메뉴_그룹;
import static kitchenpos.order.domain.OrderFixture.주문;
import static kitchenpos.order.domain.OrderStatus.COMPLETION;
import static kitchenpos.product.domain.ProductFixture.상품;
import static kitchenpos.table.domain.OrderTableFixture.주문_테이블;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import kitchenpos.common.Price;
import kitchenpos.menu.domain.Menu;
import kitchenpos.menu.domain.MenuProduct;
import kitchenpos.menu.domain.MenuRepository;
import kitchenpos.menugroup.domain.MenuGroup;
import kitchenpos.menugroup.domain.MenuGroupRepository;
import kitchenpos.order.application.OrderService;
import kitchenpos.order.application.dto.OrderCreateRequest;
import kitchenpos.order.application.dto.OrderCreateRequest.OrderLineItemCreateRequest;
import kitchenpos.order.application.dto.OrderCreateResponse;
import kitchenpos.order.application.dto.OrderResponse;
import kitchenpos.order.application.dto.OrderStatusRequest;
import kitchenpos.order.domain.OrderLineItem;
import kitchenpos.order.domain.OrderStatus;
import kitchenpos.product.domain.Product;
import kitchenpos.product.domain.ProductRepository;
import kitchenpos.table.domain.OrderTable;
import kitchenpos.table.domain.OrderTableRepository;
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
    private final OrderTableRepository orderTableRepository;
    private final ProductRepository productRepository;
    private final MenuGroupRepository menuGroupRepository;
    private final MenuRepository menuRepository;

    private Menu 저장된_메뉴;
    private OrderTable 저장된_주문_테이블;

    public OrderServiceTest(
            final OrderService orderService,
            final OrderTableRepository orderTableRepository,
            final ProductRepository productRepository,
            final MenuGroupRepository menuGroupRepository,
            final MenuRepository menuRepository
    ) {
        this.orderService = orderService;
        this.orderTableRepository = orderTableRepository;
        this.productRepository = productRepository;
        this.menuGroupRepository = menuGroupRepository;
        this.menuRepository = menuRepository;
    }

    @BeforeEach
    void setUp() {
        final OrderTable 주문_테이블 = 주문_테이블(null, null, 2, false);
        저장된_주문_테이블 = orderTableRepository.save(주문_테이블);

        final MenuGroup 메뉴_그룹 = 메뉴_그룹(null, "양념 반 후라이드 반");
        final MenuGroup 저장된_메뉴_그룹 = menuGroupRepository.save(메뉴_그룹);

        final Product 저장된_양념_치킨 = productRepository.save(상품(null, "양념 치킨", BigDecimal.valueOf(12000, 2)));
        final Product 저장된_후라이드_치킨 = productRepository.save(상품(null, "후라이드 치킨", BigDecimal.valueOf(10000, 2)));
        final MenuProduct 메뉴_상품_1 = 메뉴_상품(null, null, 저장된_양념_치킨, 1);
        final MenuProduct 메뉴_상품_2 = 메뉴_상품(null, null, 저장된_후라이드_치킨, 1);

        final Menu 메뉴 = 메뉴(null, "메뉴", BigDecimal.valueOf(22000, 2), 저장된_메뉴_그룹, List.of(메뉴_상품_1, 메뉴_상품_2));
        저장된_메뉴 = menuRepository.save(메뉴);
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
                    주문(null, 저장된_주문_테이블, OrderStatus.COOKING, LocalDateTime.now(),
                            List.of(new OrderLineItem(null, null, 저장된_메뉴.getId(), 1, 저장된_메뉴.getName(),
                                    new Price(저장된_메뉴.getPrice())))));

            assertAll(
                    () -> assertThat(저장된_주문.getId()).isNotNull(),
                    () -> assertThat(저장된_주문).usingRecursiveComparison()
                            .ignoringFields("id", "orderedTime", "orderLineItems.seq", "orderLineItems.orderId")
                            .isEqualTo(예상_응답값)
            );
        }

        @Test
        void 주문_메뉴_목록들_중_메뉴가_중복되면_예외가_발생한다() {
            // given
            final OrderCreateRequest 주문_요청값 = new OrderCreateRequest(저장된_주문_테이블.getId(),
                    List.of(new OrderLineItemCreateRequest(저장된_메뉴.getId(), 1),
                            new OrderLineItemCreateRequest(저장된_메뉴.getId(), 1)));

            // expected
            assertThatThrownBy(() -> orderService.create(주문_요청값))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메뉴 정보가 올바르지 않습니다.");
        }

        @Test
        void 주문_테이블이_존재하지_않으면_예외가_발생한다() {
            // given
            final OrderCreateRequest 주문_요청값 = new OrderCreateRequest(저장된_주문_테이블.getId() + 1,
                    List.of(new OrderLineItemCreateRequest(저장된_메뉴.getId(), 1)));

            // expected
            assertThatThrownBy(() -> orderService.create(주문_요청값))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 주문 테이블입니다.");
        }

        @Test
        void 주문_테이블이_빈_테이블이면_예외가_발생한다() {
            // given
            final OrderTable 주문_테이블 = 주문_테이블(null, null, 2, true);
            final OrderTable 저장된_주문_테이블 = orderTableRepository.save(주문_테이블);

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
                        주문(null, 저장된_주문_테이블, OrderStatus.COOKING, LocalDateTime.now(),
                                List.of(new OrderLineItem(저장된_메뉴.getId(), 1, 저장된_메뉴.getName(),
                                        new Price(저장된_메뉴.getPrice())))))
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
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 주문입니다.");
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
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 완료된 주문입니다.");
        }
    }
}
