package repository;

public interface InventoryRepository {

    void addStock(String bookId, int quantity);

    boolean isAvailable(String bookId);

    void decrement(String bookId);

    void increment(String bookId);

    int getQuantity(String bookId);
}
