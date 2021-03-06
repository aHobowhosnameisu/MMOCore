package net.Indyuce.mmocore.api.experience;

import java.util.UUID;

public class Booster {
	private final UUID uuid = UUID.randomUUID();
	private final long date = System.currentTimeMillis();
	private final Profession profession;
	private final double extra;
	private final String author;

	// length not final because boosters can stack, this allows to reduce the
	// amount of boosters
	private long length;

	public Booster(double extra, long length) {
		this(null, null, extra, length);
	}

	public Booster(String author, double extra, long length) {
		this(author, null, extra, length);
	}

	public Booster(String author, Profession profession, double extra, long length) {
		this.author = author;
		this.length = length * 1000;
		this.profession = profession;
		this.extra = extra;
	}

	public boolean isTimedOut() {
		return date + length < System.currentTimeMillis();
	}

	public long getLeft() {
		return Math.max(0, date + length - System.currentTimeMillis());
	}

	public long getCreationDate() {
		return date;
	}

	public long getLength() {
		return length;
	}

	public void addLength(long length) {
		this.length += length;
	}

	public boolean hasProfession() {
		return profession != null;
	}

	public Profession getProfession() {
		return profession;
	}

	public UUID getUniqueId() {
		return uuid;
	}

	public double calculateExp(double exp) {
		return exp * (1 + extra);
	}

	public double getExtra() {
		return extra;
	}

	public boolean hasAuthor() {
		return author != null;
	}

	public String getAuthor() {
		return author;
	}

	public boolean canStackWith(Booster booster) {
		return extra == booster.extra && (profession != null ? profession.equals(booster.getProfession()) : booster.getProfession() == null);
	}
}
