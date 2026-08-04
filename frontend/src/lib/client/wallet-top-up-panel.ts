"use client";

export const OPEN_WALLET_TOP_UP_PANEL_EVENT = "wallet-top-up:open";

export type OpenWalletTopUpPanelDetail = {
  returnPath?: string;
};

export function openWalletTopUpPanel(detail: OpenWalletTopUpPanelDetail = {}) {
  window.dispatchEvent(new CustomEvent(OPEN_WALLET_TOP_UP_PANEL_EVENT, { detail }));
}
