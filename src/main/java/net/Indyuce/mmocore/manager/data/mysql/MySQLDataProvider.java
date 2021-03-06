package net.Indyuce.mmocore.manager.data.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.configuration.ConfigurationSection;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.manager.data.DataProvider;
import net.Indyuce.mmocore.manager.data.GuildDataManager;
import net.Indyuce.mmocore.manager.data.PlayerDataManager;
import net.Indyuce.mmocore.manager.data.yaml.YAMLGuildDataManager;

public class MySQLDataProvider implements DataProvider {
	private final MySQLPlayerDataManager playerManager = new MySQLPlayerDataManager(this);
	private final YAMLGuildDataManager guildManager = new YAMLGuildDataManager();

	private final Connection connection;

	public MySQLDataProvider() {
		MySQLConfig config = new MySQLConfig(MMOCore.plugin.getConfig().getConfigurationSection("mysql"));
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection(config.getConnectionString(), config.getUser(), config.getPassword());
		} catch (ClassNotFoundException | SQLException exception) {
			throw new IllegalArgumentException("Could not initialize MySQL support: " + exception.getMessage());
		}

		executeUpdate("CREATE TABLE IF NOT EXISTS mmocore_playerdata (uuid VARCHAR(36),class_points INT(11) DEFAULT 0,skill_points INT(11) DEFAULT 0,attribute_points INT(11) DEFAULT 0,attribute_realloc_points INT(11) DEFAULT 0,level INT(11) DEFAULT 0,experience INT(11) DEFAULT 0,class VARCHAR(20),guild VARCHAR(20),last_login LONG,attributes JSON,professions JSON,quests JSON,waypoints JSON,friends JSON,skills JSON,bound_skills JSON,class_info JSON,PRIMARY KEY (uuid));");
	}

	public ResultSet getResult(String sql) {
		try {
			return connection.prepareStatement(sql).executeQuery();
		} catch (SQLException exception) {
			exception.printStackTrace();
			return null;
		}
	}

	public void executeUpdate(String sql) {
		try {
			connection.prepareStatement(sql).executeUpdate();
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
	}

	@Override
	public PlayerDataManager getDataManager() {
		return playerManager;
	}

	@Override
	public GuildDataManager getGuildManager() {
		return guildManager;
	}

	public class MySQLConfig {
		private final String database, hostname, userid, password, flags;
		private final int port;

		public MySQLConfig(ConfigurationSection config) {
			database = config.getString("database", "minecraft");
			hostname = config.getString("host", "localhost");
			port = config.getInt("port", 3306);
			userid = config.getString("user", "mmolover");
			password = config.getString("pass", "ILoveAria");
			flags = config.getString("flags", "?allowReconnect=true&useSSL=false");
		}

		public String getConnectionString() {
			return "jdbc:mysql://" + hostname + ":" + port + "/" + database + flags;
		}

		public String getUser() {
			return userid;
		}

		public String getPassword() {
			return password;
		}
	}
}
