-- Add per-user permission overrides so admins can toggle infinite currency
-- permissions (acc_infinite_credits/pixels/points) without editing their rank.

CREATE TABLE IF NOT EXISTS `users_permissions` (
  `user_id` int(11) NOT NULL,
  `permission` varchar(100) NOT NULL,
  `value` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`user_id`,`permission`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
