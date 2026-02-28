package matcher.model.type;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InputFile {
	public static List<Path> resolve(Collection<InputFile> inputFiles, Collection<Path> inputDirs) throws IOException {
		List<Path> ret = new ArrayList<>(inputFiles.size());

		for (InputFile inputFile : inputFiles) {
			ret.add(inputFile.resolve(inputDirs));
		}

		return ret;
	}

	InputFile(Path path) {
		try {
			this.path = path;
			this.fileName = getSanitizedFileName(path);
			this.size = Files.size(path);
			this.hash = HashType.SHA256.hash(path);
			this.hashType = HashType.SHA256;
			this.pathHint = path;
			this.url = null;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public InputFile(String fileName, String url) {
		this(fileName, unknownSize, null, null, url);
	}

	public InputFile(String fileName, Path pathHint, String url) {
		this(fileName, unknownSize, null, null, pathHint, url);
	}

	public InputFile(String fileName, byte[] hash, HashType hashType, String url) {
		this(fileName, unknownSize, hash, hashType, url);
	}

	public InputFile(String fileName, byte[] hash, HashType hashType, Path pathHint, String url) {
		this(fileName, unknownSize, hash, hashType, pathHint, url);
	}

	public InputFile(String fileName, long size, byte[] hash, HashType hashType, String url) {
		this(fileName, size, hash, hashType, null, url);
	}

	public InputFile(String fileName, long size, byte[] hash, HashType hashType, Path pathHint, String url) {
		this.path = null;
		this.fileName = fileName;
		this.size = size;
		this.hash = hash;
		this.hashType = hashType;
		this.pathHint = pathHint;
		this.url = url;
	}

	public Path resolve(Collection<Path> inputDirs) throws IOException {
		Path dlTmp = getDlTmp(false);

		if (pathHint != null) {
			if (pathHint.isAbsolute()) {
				if (Files.isRegularFile(pathHint) && equals(pathHint)) {
					return pathHint;
				}
			} else {
				for (Path inputDir : inputDirs) {
					Path file = inputDir.resolve(pathHint);

					if (Files.isRegularFile(file) && equals(file)) {
						return file;
					}
				}

				if (dlTmp != null) {
					Path file = dlTmp.resolve(pathHint);

					if (Files.isRegularFile(file) && equals(file)) {
						return file;
					}
				}
			}
		}

		AtomicReference<Path> res = new AtomicReference<>();

		for (Path inputDir : inputDirs) {
			Files.walkFileTree(inputDir, new SimpleFileVisitor<>() {
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (InputFile.this.equals(file)) {
						res.set(file);

						return FileVisitResult.TERMINATE;
					}

					return FileVisitResult.CONTINUE;
				}
			});

			Path ret = res.get();
			if (ret != null) return ret;
		}

		if (dlTmp != null) {
			for (Path path : Files.newDirectoryStream(dlTmp)) {
				if (equals(path)) return path;
			}
		}

		if (url != null) {
			try {
				return downloadToTmp(url);
			} catch (IOException | InterruptedException e) {
				throw new IOException("download of "+url+" failed: "+e.toString());
			}
		}

		throw new IOException("can't find input "+this);
	}

	public boolean hasPath() {
		return path != null;
	}

	public boolean equals(Path path) {
		try {
			if (this.path != null) return Files.isSameFile(path, this.path);

			if (fileName != null && !getSanitizedFileName(path).equals(fileName)) return false;
			if (size != -1 && Files.size(path) != size) return false;

			return hash == null || Arrays.equals(hash, hashType.hash(path));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof InputFile)) return false;

		InputFile o = (InputFile) obj;

		try {
			return (path == null || o.path == null || Files.isSameFile(path, o.path))
					&& (fileName == null || o.fileName == null || fileName.equals(o.fileName))
					&& (size < 0 || o.size < 0 || size == o.size)
					&& (hash == null || o.hash == null || hashType == o.hashType && Arrays.equals(hash, o.hash));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public String toString() {
		if (fileName != null) {
			return fileName;
		} else if (pathHint != null) {
			return pathHint.getFileName().toString();
		} else if (hash != null && hash.length >= 8) {
			return Long.toUnsignedString((hash[0] & 0xffL) << 56 | (hash[1] & 0xffL) << 48 | (hash[2] & 0xffL) << 40 | (hash[3] & 0xffL) << 32
					| (hash[4] & 0xffL) << 24 | (hash[5] & 0xffL) << 16 | (hash[6] & 0xffL) << 8 | hash[7] & 0xffL, 16);
		} else {
			return "unknown";
		}
	}

	private static String getSanitizedFileName(Path path) {
		return path.getFileName().toString().replace('\n', ' ');
	}

	public enum HashType {
		SHA1("SHA-1"),
		SHA256("SHA-256");

		HashType(String algorithm) {
			this.algorithm = algorithm;
		}

		public MessageDigest createDigest() {
			try {
				return MessageDigest.getInstance(algorithm);
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		}

		public byte[] hash(Path path) throws IOException {
			TlData tlData = tlDatas.get();

			MessageDigest digest = tlData.digests.get(this);
			ByteBuffer buffer = tlData.buffer;
			buffer.clear();

			try (SeekableByteChannel channel = Files.newByteChannel(path)) {
				while (channel.read(buffer) != -1) {
					buffer.flip();
					digest.update(buffer);
					buffer.clear();
				}
			}

			return digest.digest();
		}

		private static class TlData {
			TlData() {
				for (HashType type : HashType.values()) {
					digests.put(type, type.createDigest());
				}

				buffer = ByteBuffer.allocate(256 * 1024);
			}

			final Map<HashType, MessageDigest> digests = new EnumMap<>(HashType.class);
			final ByteBuffer buffer;
		}

		private static final ThreadLocal<TlData> tlDatas = ThreadLocal.withInitial(TlData::new);

		public final String algorithm;
	}


	private static synchronized Path getDlTmp(boolean create) {
		Path ret = dlTmp;
		if (ret != null || !create) return ret;

		Path dir = Paths.get("dlTmp");
		int suffix = 0;
		Path path;

		do {
			path = dir.resolve(Integer.toString(suffix++));
		} while (Files.exists(path));

		try {
			Files.createDirectories(path);
			ret = path.toRealPath();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		final Path res = ret;

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					Files.walkFileTree(res, new SimpleFileVisitor<>() {
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							System.out.println("delete file "+file);

							if (file.normalize().toAbsolutePath().startsWith(res)) {
								Files.delete(file);
							}

							return FileVisitResult.CONTINUE;
						}

						public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							System.out.println("delete dir "+dir);

							if (dir.normalize().toAbsolutePath().startsWith(res)) {
								Files.delete(dir);
							}

							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		dlTmp = ret;

		return ret;
	}

	private static Path downloadToTmp(String url) throws IOException, InterruptedException {
		LOGGER.info("downloading {}", url);

		String name = url.substring(url.lastIndexOf('/') + 1).replaceAll("[^\\w\\.\\- ]", "x");
		if (name.isEmpty()) name = "dl";
		int suffix = 0;
		Path dlTmp = getDlTmp(true);
		Path out = dlTmp.resolve(name);

		while (Files.exists(out)) {
			out = dlTmp.resolve(name + "_" + (++suffix));
		}

		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).build();
		HttpResponse<InputStream> response = HTTP_CLIENT.send(request, BodyHandlers.ofInputStream());

		if (response.statusCode() != 200) {
			throw new IOException("bad http status code: "+response.statusCode());
		}

		try (InputStream is = response.body()) {
			Files.copy(is, out);
		}

		return out;
	}

	public static final long unknownSize = -1;

	private static final Logger LOGGER = LoggerFactory.getLogger(InputFile.class);
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

	private static Path dlTmp;

	public final Path path;
	public final String fileName;
	public final long size;
	public final byte[] hash;
	public final HashType hashType;
	public final Path pathHint;
	public final String url;
}
