package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.Product;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Stores product inventory as a readable tab-separated text file. */
public class ProductDAO {

  private static final Path DATA_DIRECTORY = Paths.get("data");
  private static final Path PRODUCT_FILE = DATA_DIRECTORY.resolve("products.txt");

  public void saveToFile(ListInterface<Product> productList) {
    try {
      Files.createDirectories(DATA_DIRECTORY);
      try (BufferedWriter writer = Files.newBufferedWriter(PRODUCT_FILE, StandardCharsets.UTF_8)) {
        writer.write("productNumber\tproductName\tquantity");
        writer.newLine();
        for (int i = 1; i <= productList.getNumberOfEntries(); i++) {
          Product product = productList.getEntry(i);
          writer.write(clean(product.getNumber()) + "\t" + clean(product.getName()) + "\t" + product.getQuantity());
          writer.newLine();
        }
      }
    } catch (IOException ex) {
      System.out.println("\n[ERROR] Cannot save products to " + PRODUCT_FILE + ": " + ex.getMessage());
    }
  }

  public ListInterface<Product> retrieveFromFile() {
    ListInterface<Product> productList = new ArrayList<>();
    if (!Files.exists(PRODUCT_FILE)) return productList;
    try (BufferedReader reader = Files.newBufferedReader(PRODUCT_FILE, StandardCharsets.UTF_8)) {
      reader.readLine();
      String line;
      while ((line = reader.readLine()) != null) {
        String[] field = line.split("\\t", -1);
        if (field.length == 3) productList.add(new Product(field[0], field[1], Integer.parseInt(field[2])));
      }
    } catch (IOException | IllegalArgumentException ex) {
      System.out.println("\n[ERROR] Cannot read products from " + PRODUCT_FILE + ": " + ex.getMessage());
      productList.clear();
    }
    return productList;
  }

  private String clean(String value) {
    return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
  }
}
