package warehouse_inventory.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import warehouse_inventory.entity.Item;
import warehouse_inventory.repository.ItemRepository;

@Service
public class ItemService {

    private final ItemRepository repository;

    public ItemService(ItemRepository repository) {
        this.repository = repository;
    }

    // Get all items
    public List<Item> getAllItems() {
        return repository.findAll();
    }

    // Get one item by ID
    public Item getItemById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item not found"));
    }

    // Add a new item
    @Transactional
    public Item addItem(Item item) {

        validateItem(item);

        if (repository.existsByItemCodeIgnoreCase(item.getItemCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Item code already exists");
        }

        Item newItem = new Item();

        copyFields(item, newItem);
        newItem.setActive(true);

        return repository.save(newItem);
    }

    // Update an existing item
    @Transactional
    public Item updateItem(Long id, Item item) {

        Item existing = getItemById(id);

        validateItem(item);

        if (repository.existsByItemCodeIgnoreCaseAndIdNot(
                item.getItemCode(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Item code already exists");
        }

        copyFields(item, existing);

        return repository.save(existing);
    }

    // Deactivate instead of permanently deleting
    @Transactional
    public void deleteItem(Long id) {

        Item item = getItemById(id);

        item.setActive(false);

        repository.save(item);
    }

    // Validate input
    private void validateItem(Item item) {

        if (item.getItemCode() == null ||
                item.getItemCode().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Item code is required");
        }

        if (item.getItemName() == null ||
                item.getItemName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Item name is required");
        }

        if (item.getReorderLevel() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reorder level cannot be negative");
        }
    }

    // Copy editable fields
    private void copyFields(Item source, Item target) {

        target.setItemCode(source.getItemCode().trim());
        target.setItemName(source.getItemName().trim());
        target.setCategory(source.getCategory());
        target.setUnit(source.getUnit());
        target.setReorderLevel(source.getReorderLevel());
    }
}