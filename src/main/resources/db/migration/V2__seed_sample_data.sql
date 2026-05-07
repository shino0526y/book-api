INSERT INTO authors(name, birth_date)
VALUES
    ('Jane Doe', DATE '1903-06-25'),
('John Doe', DATE '1894-07-26'),
('山田 太郎', DATE '1948-04-28'),
('山田 花子', DATE '1960-11-10');

INSERT INTO books(title, price, publication_status)
VALUES
    ('Sashimi is Great', 1200.00, 'PUBLISHED'),
('Run Away', 1100.00, 'PUBLISHED'),
('植物図鑑', 1500.00, 'PUBLISHED'),
('プログラミング言語史 上巻', 1800.00, 'UNPUBLISHED');

INSERT INTO book_authors(book_id, author_id)
SELECT
    books.id,
    authors.id
FROM (
    VALUES ('Sashimi is Great', 'Jane Doe'),
('Run Away', 'John Doe'),
('植物図鑑', '山田 太郎'),
('植物図鑑', '山田 花子'),
('プログラミング言語史 上巻', '山田 花子')) AS seed(book_title, author_name)
    JOIN books ON books.title = seed.book_title
    JOIN authors ON authors.name = seed.author_name;

