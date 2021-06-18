package ch.admin.bag.covidcertificate.backend.delivery.data.util;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.stream.Collectors;

public class CodeGenerator {

    private static List<Integer> ALPHABET =
            List.of(
                            'A', 'B', 'C', 'D', 'E', 'F', 'H', 'K', 'M', 'N', 'P', 'R', 'S', 'T',
                            'U', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8',
                            '9')
                    .stream()
                    .map(c -> (int) c)
                    .collect(Collectors.toList());
    private static Integer LEFT_LIMIT =
            ALPHABET.stream().mapToInt(i -> i).min().orElseThrow(NoSuchElementException::new);
    private static Integer RIGHT_LIMIT =
            ALPHABET.stream().mapToInt(i -> i).max().orElseThrow(NoSuchElementException::new);

    public static String generateCode() {
        Random random = new Random();
        int length = 9;

        return random.ints(LEFT_LIMIT, RIGHT_LIMIT + 1)
                .filter(i -> ALPHABET.contains(i))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
