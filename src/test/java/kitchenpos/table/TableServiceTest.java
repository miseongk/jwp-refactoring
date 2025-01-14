package kitchenpos.table;

import static kitchenpos.menu.domain.MenuFixture.메뉴;
import static kitchenpos.menu.domain.MenuProductFixture.메뉴_상품;
import static kitchenpos.menugroup.domain.MenuGroupFixture.메뉴_그룹;
import static kitchenpos.order.domain.OrderFixture.주문;
import static kitchenpos.product.domain.ProductFixture.상품;
import static kitchenpos.tablegroup.domain.TableGroupFixture.테이블_그룹;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import kitchenpos.menu.domain.Menu;
import kitchenpos.menu.domain.MenuProduct;
import kitchenpos.menu.domain.MenuRepository;
import kitchenpos.menugroup.domain.MenuGroup;
import kitchenpos.menugroup.domain.MenuGroupRepository;
import kitchenpos.order.domain.Order;
import kitchenpos.order.domain.OrderCompletedValidatorImpl;
import kitchenpos.order.domain.OrderRepository;
import kitchenpos.order.domain.OrderStatus;
import kitchenpos.product.domain.Product;
import kitchenpos.product.domain.ProductRepository;
import kitchenpos.table.application.TableService;
import kitchenpos.table.application.dto.OrderTableCreateRequest;
import kitchenpos.table.application.dto.OrderTableCreateResponse;
import kitchenpos.table.application.dto.OrderTableEmptyRequest;
import kitchenpos.table.application.dto.OrderTableNumberOfGuestsRequest;
import kitchenpos.table.application.dto.OrderTableResponse;
import kitchenpos.table.domain.OrderTable;
import kitchenpos.table.domain.OrderTableRepository;
import kitchenpos.table.domain.OrderTableValidator;
import kitchenpos.tablegroup.domain.TableGroup;
import kitchenpos.tablegroup.domain.TableGroupRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@Sql("classpath:truncate.sql")
@TestConstructor(autowireMode = AutowireMode.ALL)
class TableServiceTest {

    private final TableService tableService;
    private final OrderTableRepository orderTableRepository;
    private final ProductRepository productRepository;
    private final MenuGroupRepository menuGroupRepository;
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final TableGroupRepository tableGroupRepository;
    private final OrderTableValidator orderTableValidator;

    public TableServiceTest(
            final TableService tableService,
            final OrderTableRepository orderTableRepository,
            final ProductRepository productRepository,
            final MenuGroupRepository menuGroupRepository,
            final MenuRepository menuRepository,
            final OrderRepository orderRepository,
            final TableGroupRepository tableGroupRepository,
            final OrderTableValidator orderTableValidator) {
        this.tableService = tableService;
        this.orderTableRepository = orderTableRepository;
        this.productRepository = productRepository;
        this.menuGroupRepository = menuGroupRepository;
        this.menuRepository = menuRepository;
        this.orderRepository = orderRepository;
        this.tableGroupRepository = tableGroupRepository;
        this.orderTableValidator = new OrderTableValidator(
                new OrderCompletedValidatorImpl(orderRepository)
        );
    }

    @Test
    void 주문_테이블을_정상적으로_등록한다() {
        // given
        final OrderTableCreateRequest 주문_테이블_요청값 = new OrderTableCreateRequest(2, false);

        // when
        final OrderTableCreateResponse 주문_테이블_응답값 = tableService.create(주문_테이블_요청값);

        // then
        final OrderTableCreateResponse 예상_응답값 = OrderTableCreateResponse.of(new OrderTable(2, false));

        assertAll(
                () -> assertThat(주문_테이블_응답값.getId()).isNotNull(),
                () -> assertThat(주문_테이블_응답값).usingRecursiveComparison()
                        .ignoringFields("id")
                        .isEqualTo(예상_응답값)
        );
    }

    @Test
    void 주문_테이블_목록을_정상적으로_조회한다() {
        // given
        tableService.create(new OrderTableCreateRequest(2, false));
        tableService.create(new OrderTableCreateRequest(5, false));

        // when
        final List<OrderTableResponse> 주문_테이블들 = tableService.list();

        // then
        final OrderTableResponse 주문_테이블1_응답값 = OrderTableResponse.of(new OrderTable(2, false));
        final OrderTableResponse 주문_테이블2_응답값 = OrderTableResponse.of(new OrderTable(5, false));

        assertThat(주문_테이블들).usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(List.of(주문_테이블1_응답값, 주문_테이블2_응답값));
    }

    @Nested
    class 주문_테이블을_빈_테이블로_변경_시 {

        @Test
        void 주문_테이블을_빈_테이블로_변경한다() {
            // given
            final OrderTableCreateResponse 저장된_주문_테이블 = tableService.create(new OrderTableCreateRequest(2, false));

            // when
            final OrderTableResponse 빈_테이블로_변경된_주문_테이블 = tableService.changeEmpty(저장된_주문_테이블.getId(),
                    new OrderTableEmptyRequest(true));

            // then
            assertThat(빈_테이블로_변경된_주문_테이블.isEmpty()).isTrue();
        }

        @Test
        void 주문_테이블이_존재하지_않으면_예외가_발생한다() {
            // given
            final OrderTableCreateResponse 저장된_주문_테이블 = tableService.create(new OrderTableCreateRequest(2, false));

            // expected
            assertThatThrownBy(() -> tableService.changeEmpty(저장된_주문_테이블.getId() + 1, new OrderTableEmptyRequest(true)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 주문 테이블 입니다. id = ");
        }

        @Test
        void 주문_테이블이_이미_테이블_그룹에_속해있으면_예외가_발생한다() {
            // given
            final OrderTableCreateResponse 저장된_주문_테이블1 = tableService.create(new OrderTableCreateRequest(2, true));
            final OrderTableCreateResponse 저장된_주문_테이블2 = tableService.create(new OrderTableCreateRequest(3, true));

            final OrderTable 주문_테이블1 = new OrderTable(저장된_주문_테이블1.getId(), null, 2, true);
            final OrderTable 주문_테이블2 = new OrderTable(저장된_주문_테이블2.getId(), null, 3, true);
            final TableGroup 테이블_그룹 = 테이블_그룹(null, null);

            final TableGroup 저장된_테이블_그룹 = tableGroupRepository.save(테이블_그룹);
            주문_테이블1.makeTableGroup(저장된_테이블_그룹.getId());
            주문_테이블2.makeTableGroup(저장된_테이블_그룹.getId());

            orderTableRepository.save(주문_테이블1);
            orderTableRepository.save(주문_테이블2);

            // expected
            assertThatThrownBy(() -> tableService.changeEmpty(저장된_주문_테이블1.getId(), new OrderTableEmptyRequest(true)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("테이블이 그룹에 속해있으면 빈 테이블로 변경할 수 없습니다.");
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"COOKING", "MEAL"})
        void 주문_상태가_COOKING_또는_MEAL이면_예외가_발생한다(final OrderStatus 주문_상태) {
            // given
            final OrderTableCreateResponse 저장된_주문_테이블 = tableService.create(new OrderTableCreateRequest(2, true));

            주문_등록(저장된_주문_테이블, 주문_상태);

            // expected
            assertThatThrownBy(() -> tableService.changeEmpty(저장된_주문_테이블.getId(), new OrderTableEmptyRequest(true)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문이 완료되지 않았습니다.");
        }
    }

    @Nested
    class 주문_테이블의_방문_손님수_변경_시 {

        @Test
        void 주문_테이블의_방문_손님수를_정상적으로_변경한다() {
            // given
            final OrderTableCreateResponse 주문_테이블 = tableService.create(new OrderTableCreateRequest(2, false));
            final OrderTableNumberOfGuestsRequest 변경할_주문_테이블 = new OrderTableNumberOfGuestsRequest(5);

            // when
            final OrderTableResponse 방문_손님수가_변경된_주문_테이블 = tableService.changeNumberOfGuests(주문_테이블.getId(), 변경할_주문_테이블);

            // then
            assertThat(방문_손님수가_변경된_주문_테이블.getNumberOfGuests()).isEqualTo(5);
        }

        @Test
        void 변경할_주문_테이블이_존재하지_않으면_예외가_발생한다() {
            // given
            final OrderTableCreateResponse 주문_테이블 = tableService.create(new OrderTableCreateRequest(2, false));
            final OrderTableNumberOfGuestsRequest 변경할_주문_테이블 = new OrderTableNumberOfGuestsRequest(5);

            // expected
            assertThatThrownBy(() -> tableService.changeNumberOfGuests(주문_테이블.getId() + 1, 변경할_주문_테이블))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 주문 테이블 입니다. id = ");
        }

        @Test
        void 입력받은_주문_테이블이_비어있으면_예외가_발생한다() {
            // given
            final OrderTableCreateResponse 주문_테이블 = tableService.create(new OrderTableCreateRequest(2, true));
            final OrderTableNumberOfGuestsRequest 변경할_주문_테이블 = new OrderTableNumberOfGuestsRequest(5);

            // expected
            assertThatThrownBy(() -> tableService.changeNumberOfGuests(주문_테이블.getId(), 변경할_주문_테이블))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문 테이블이 비어있으면 방문자 수를 변경할 수 없습니다.");
        }

    }

    private void 주문_등록(final OrderTableCreateResponse 저장된_주문_테이블, final OrderStatus 주문_상태) {
        final MenuGroup 메뉴_그룹 = 메뉴_그룹(null, "양념 반 후라이드 반");
        final MenuGroup 저장된_메뉴_그룹 = menuGroupRepository.save(메뉴_그룹);

        final Product 저장된_양념_치킨 = productRepository.save(상품(null, "양념 치킨", BigDecimal.valueOf(12000, 2)));
        final Product 저장된_후라이드_치킨 = productRepository.save(상품(null, "후라이드 치킨", BigDecimal.valueOf(10000, 2)));
        final MenuProduct 메뉴_상품_1 = 메뉴_상품(null, null, 저장된_양념_치킨, 1);
        final MenuProduct 메뉴_상품_2 = 메뉴_상품(null, null, 저장된_후라이드_치킨, 1);

        final Menu 메뉴 = 메뉴(null, "메뉴", BigDecimal.valueOf(22000, 2), 저장된_메뉴_그룹, List.of(메뉴_상품_1, 메뉴_상품_2));
        menuRepository.save(메뉴);

        final OrderTable 주문_테이블 = orderTableRepository.findById(저장된_주문_테이블.getId()).get();
        주문_테이블.changeEmpty(false, orderTableValidator);
        final Order 주문 = 주문(null, 주문_테이블, 주문_상태, null, Collections.emptyList());
        orderRepository.save(주문);
    }
}
