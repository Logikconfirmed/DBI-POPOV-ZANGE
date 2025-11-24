package com.example.dbi.BüchereiVerwaltung.web;

import com.example.dbi.BüchereiVerwaltung.modelMongo.BookDocument;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/mongo/books")
public class MongoBookController {

    private final MongoBookService mongoBookService;

    public MongoBookController(MongoBookService mongoBookService) {
        this.mongoBookService = mongoBookService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("books", mongoBookService.listAll());
        model.addAttribute("bookForm", new BookForm());
        return "books/list";
    }

    @PostMapping
    public String create(@ModelAttribute BookForm bookForm) {
        mongoBookService.create(bookForm);
        return "redirect:/mongo/books";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable String id, Model model) {
        BookDocument document = mongoBookService.findById(id);
        model.addAttribute("bookId", id);
        model.addAttribute("bookForm", mongoBookService.toForm(document));
        return "books/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable String id, @ModelAttribute BookForm form) {
        mongoBookService.update(id, form);
        return "redirect:/mongo/books";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id) {
        mongoBookService.delete(id);
        return "redirect:/mongo/books";
    }
}

