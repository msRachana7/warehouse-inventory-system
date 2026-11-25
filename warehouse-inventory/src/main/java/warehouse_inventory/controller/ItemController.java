package warehouse_inventory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import warehouse_inventory.entity.Item;
import warehouse_inventory.service.ItemService;

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = "http://localhost:4200")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    // GET all items
    @GetMapping
    public List<Item> getAllItems() {
        return service.getAllItems();
    }

    // GET item by ID
    @GetMapping("/{id}")
    public Item getItemById(@PathVariable Long id) {
        return service.getItemById(id);
    }

    // POST new item
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Item addItem(@RequestBody Item item) {
        return service.addItem(item);
    }

    // PUT update item
    @PutMapping("/{id}")
    public Item updateItem(
            @PathVariable Long id,
            @RequestBody Item item) {

        return service.updateItem(id, item);
    }

    // DELETE (deactivate) item
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long id) {
        service.deleteItem(id);
    }
}