package demo;
public record OrderSummary(String reference, int quantity) {
    public OrderSummary {
        if (quantity < 0) throw new IllegalArgumentException("quantity");
    }
    public boolean empty() { return quantity == 0; }
}
