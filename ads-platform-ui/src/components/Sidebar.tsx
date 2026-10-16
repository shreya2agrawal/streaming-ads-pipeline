import React from "react";
import { useLocation, useNavigate } from "react-router-dom";

interface NavItem {
    path: string;
    label: string;
    icon: string;
}

const navItems: NavItem[] = [
    { path: "/overview", label: "Overview", icon: "ti-layout-dashboard" },
    { path: "/campaigns", label: "Campaigns", icon: "ti-speakerphone" },
    { path: "/creatives", label: "Creatives", icon: "ti-photo" },
    { path: "/inventory", label: "Inventory", icon: "ti-box" },
    { path: "/placements", label: "Placements", icon: "ti-layout-grid" },
    { path: "/safety", label: "Brand safety", icon: "ti-shield-check" },
    { path: "/plans", label: "Media plans", icon: "ti-file-description" },
    { path: "/insights", label: "Insights", icon: "ti-chart-bar" },
    { path: "/demo-player", label: "Demo player", icon: "ti-player-play" },
];

export default function Sidebar() {
    const location = useLocation();
    const navigate = useNavigate();

    return (
        <div
            style={{
                width: 200,
                minWidth: 200,
                background: "#141414",
                borderRight: "0.5px solid #1e1e1e",
                display: "flex",
                flexDirection: "column",
                height: "100vh",
            }}
        >
            {/* {logo} */}
            <div
                style={{
                    padding: "16px 16px 14px",
                    borderBottom: "0.5px solid #1e1e1e",
                    marginBottom: 8,
                }}
            >
                <div
                    style={{
                        fontSize: 22,
                        fontWeight: 700,
                        color: "#E50914",
                        letterSpacing: -1,
                    }}
                >
                    N
                </div>
                <div
                    style={{
                        fontSize: 11,
                        color: "#888",
                        marginTop: 2,
                    }}
                >
                    Ads Platform
                </div>
            </div>
            {/* Nav Items */}
            <div style={{ flex: 1 }}>
                {navItems.map((item) => {
                    const isActive = location.pathname === item.path;
                    return (
                        <div
                            key={item.path}
                            onClick={() => navigate(item.path)}
                            style={{
                                display: "flex",
                                alignItems: "center",
                                gap: 8,
                                padding: "9px 16px",
                                fontSize: 13,
                                color: isActive ? "#e5e5e5" : "#888",
                                background: isActive
                                    ? "#1a1a1a"
                                    : "transparent",
                                borderRight: isActive
                                    ? "2px solid #E50914"
                                    : "2px solid transparent",
                                cursor: "pointer",
                                transition: "all 0.15s",
                            }}
                            onMouseEnter={(e) => {
                                if (!isActive) {
                                    (e.currentTarget as HTMLDivElement).style
                                        .color = "#e5e5e5";
                                    (e.currentTarget as HTMLDivElement).style
                                        .background = "#1a1a1a";
                                }
                            }}
                            onMouseLeave={(e) => {
                                if (!isActive) {
                                    (e.currentTarget as HTMLDivElement).style
                                        .color = "#888";
                                    (e.currentTarget as HTMLDivElement).style
                                        .background = "transparent";
                                }
                            }}
                        >
                            <i
                                className={`ti ${item.icon}`}
                                aria-hidden="true"
                                style={{ fontSize: 16 }}
                            />
                            {item.label}
                        </div>
                    );
                })}
            </div>
            {/* Settings at bottom */}
            <div
                style={{
                    padding: "9px 16px",
                    fontSize: 13,
                    color: "#888",
                    display: "flex",
                    alignItems: "center",
                    gap: 8,
                    cursor: "pointer",
                    borderTop: "0.5px solid #1e1e1e",
                }}
            >
                <i
                    className="ti ti-settings"
                    aria-hidden="true"
                    style={{ fontSize: 16 }}
                />
                Settings
            </div>
        </div>
    );
}
