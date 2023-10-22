package kitchenpos.application.dto.table;

import kitchenpos.domain.OrderTable;

public class OrderTableCreateResponse {

    private final Long id;
    private final Long tableGroupId;
    private final int numberOfGuests;
    private final boolean empty;

    public OrderTableCreateResponse(final Long id, final Long tableGroupId, final int numberOfGuests,
                                    final boolean empty) {
        this.id = id;
        this.tableGroupId = tableGroupId;
        this.numberOfGuests = numberOfGuests;
        this.empty = empty;
    }

    public static OrderTableCreateResponse of(final OrderTable orderTable) {
        if (orderTable.getTableGroup() == null) {
            return new OrderTableCreateResponse(
                    orderTable.getId(),
                    null,
                    orderTable.getNumberOfGuests(),
                    orderTable.isEmpty()
            );
        }
        return new OrderTableCreateResponse(
                orderTable.getId(),
                orderTable.getTableGroup().getId(),
                orderTable.getNumberOfGuests(),
                orderTable.isEmpty()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getTableGroupId() {
        return tableGroupId;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public boolean isEmpty() {
        return empty;
    }
}
