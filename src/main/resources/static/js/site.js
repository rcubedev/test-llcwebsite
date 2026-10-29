// All the site's interactivity. Plain JS, no libraries, no build step.
// Each section below is independent and only runs if its markup is on the page.

(() => {
  "use strict";

  /* ------------------------------------------------------------------
     Popups: [data-modal="name"], closed via [data-modal-close],
     clicking the backdrop, or Escape.
  ------------------------------------------------------------------ */

  // The registration form (a Fillout iframe) starts loading as soon as someone shows interest:
  // hovering or focusing a Register button, or touching it on a phone. By the time they click,
  // it's usually there. Until it is, the popup shows a spinner.
  function loadApplyForm() {
    const popup = document.querySelector("[data-modal=apply]");
    const frame = popup?.querySelector("iframe");
    if (!frame || frame.src) return;
    frame.addEventListener("load", () => popup.classList.remove("is-loading"), { once: true });
    frame.src = frame.dataset.src;
  }

  for (const type of ["pointerover", "focusin", "touchstart"]) {
    document.addEventListener(type, (e) => {
      if (e.target.closest?.("[data-open-apply]")) loadApplyForm();
    }, { passive: true });
  }

  const modalHooks = {
    apply: {
      open() {
        loadApplyForm(); // already loading if the button was hovered first
        history.replaceState(null, "", location.pathname + "#apply");
      },
      close() {
        history.replaceState(null, "", location.pathname);
      },
    },
    sponsor: {
      open() {
        history.replaceState(null, "", location.pathname + "#contact");
      },
      close() {
        history.replaceState(null, "", location.pathname);
      },
    },
    video: {
      open() {
        const slot = document.querySelector("[data-video-slot]");
        const frame = document.createElement("iframe");
        frame.src = slot.dataset.src;
        frame.title = "Limelight intro video";
        frame.className = "h-full w-full";
        frame.allow = "autoplay; encrypted-media; picture-in-picture; fullscreen";
        frame.allowFullscreen = true;
        slot.replaceChildren(frame);
      },
      close() {
        document.querySelector("[data-video-slot]").replaceChildren(); // stops playback
      },
    },
  };

  let openModal = null;

  // Popups with data-fade fade in and out (opacity transition in CSS, toggled by .is-open).
  const FADE_MS = 250;

  function showModal(name) {
    const modal = document.querySelector(`[data-modal="${name}"]`);
    if (!modal || openModal === modal) return;
    if (openModal) hideModal();
    clearTimeout(modal.hideTimer);
    modal.hidden = false;
    if (modal.hasAttribute("data-fade")) {
      modal.getBoundingClientRect(); // apply the starting opacity before fading in
      modal.classList.add("is-open");
    }
    document.body.classList.add("modal-open");
    openModal = modal;
    modalHooks[name]?.open?.();
  }

  function hideModal() {
    if (!openModal) return;
    const modal = openModal;
    const name = modal.dataset.modal;
    openModal = null;
    document.body.classList.remove("modal-open");
    if (modal.hasAttribute("data-fade")) {
      modal.classList.remove("is-open");
      modal.hideTimer = setTimeout(() => (modal.hidden = true), FADE_MS);
    } else {
      modal.hidden = true;
    }
    modalHooks[name]?.close?.();
  }

  document.addEventListener("click", (e) => {
    const opener = e.target.closest("[data-open-apply], [data-open-sponsor], [data-open-video]");
    if (opener) {
      e.preventDefault();
      if (opener.hasAttribute("data-open-apply")) showModal("apply");
      else if (opener.hasAttribute("data-open-sponsor")) showModal("sponsor");
      else showModal("video");
      return;
    }
    if (!openModal) return;
    // Backdrop = the modal element itself (or the area around the form); content sits inside it.
    const backdrop = e.target === openModal || e.target.classList.contains("apply-popup-main");
    if (backdrop || e.target.closest("[data-modal-close]")) hideModal();
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") hideModal();
  });

  // Links like /#apply or /#contact open the matching popup.
  function openFromHash() {
    if (location.hash === "#apply") showModal("apply");
    else if (location.hash === "#contact" && document.querySelector("[data-modal=sponsor]")) showModal("sponsor");
  }

  /* ------------------------------------------------------------------
     Navbar: slides away when scrolling down, back when scrolling up.
     Desktop only; on mobile it stays put.
  ------------------------------------------------------------------ */

  const navbar = document.querySelector("[data-navbar]");
  if (navbar) {
    let lastY = window.scrollY;
    const setHidden = (hidden) => {
      navbar.classList.toggle("-translate-y-[calc(100%+1rem)]", hidden);
      navbar.classList.toggle("translate-y-0", !hidden);
    };

    window.addEventListener("scroll", () => {
      if (window.innerWidth < 768) {
        setHidden(false);
        return;
      }
      const y = window.scrollY;
      setHidden(y >= 50 && y > lastY);
      lastY = y;
    }, { passive: true });
  }

  /* ------------------------------------------------------------------
     Stacking cards: each card scrolls normally until its bottom reaches
     the bottom of the screen, then stays pinned (position: sticky) while
     the next card slides over it. Pinning starts here, so without JS the
     cards just scroll like a normal page.
  ------------------------------------------------------------------ */

  const cardSection = document.querySelector("[data-scroll-cards]");

  function cardAnchorTop(id) {
    const anchor = document.querySelector(`[data-card-anchor="${CSS.escape(id)}"]`);
    const target = anchor ?? document.getElementById(id);
    return target ? target.getBoundingClientRect().top + window.scrollY : null;
  }

  function scrollToCard(id, behavior = "smooth") {
    const top = cardAnchorTop(id);
    if (top !== null) window.scrollTo({ top, behavior });
  }

  if (cardSection) {
    const cards = [...cardSection.querySelectorAll("[data-card]")];

    // Measures the *small* viewport height (100svh), so the mobile address bar
    // showing and hiding doesn't make the cards jump.
    const viewport = document.createElement("div");
    viewport.style.cssText = "position:fixed;top:0;left:0;width:0;height:100svh;visibility:hidden;pointer-events:none";
    document.body.append(viewport);

    const pin = () => {
      const screen = viewport.offsetHeight;
      for (const card of cards) {
        card.style.position = "sticky";
        card.style.top = `${Math.min(0, screen - card.offsetHeight)}px`;
      }
    };

    pin();
    new ResizeObserver(pin).observe(cardSection);
    window.addEventListener("resize", pin);
  }

  // In-page links (#about, #timeline, ...) scroll to where the card starts.
  document.addEventListener("click", (e) => {
    const link = e.target.closest('a[href^="#"], a[href^="/#"]');
    if (!link || e.defaultPrevented) return;
    const id = link.getAttribute("href").replace(/^\/?#/, "");
    if (!id || !document.getElementById(id)) return;
    e.preventDefault();
    history.pushState(null, "", `#${id}`);
    scrollToCard(id);
  });

  window.addEventListener("hashchange", () => {
    const id = location.hash.slice(1);
    if (id && document.getElementById(id)) scrollToCard(id);
    openFromHash();
  });

  /* ------------------------------------------------------------------
     FAQ: one answer open at a time. The styling follows aria-expanded.
  ------------------------------------------------------------------ */

  for (const accordion of document.querySelectorAll("[data-accordion]")) {
    accordion.addEventListener("click", (e) => {
      const toggle = e.target.closest("[data-accordion-toggle]");
      if (!toggle) return;
      const opening = toggle.getAttribute("aria-expanded") !== "true";
      for (const other of accordion.querySelectorAll("[data-accordion-toggle]")) {
        other.setAttribute("aria-expanded", "false");
      }
      toggle.setAttribute("aria-expanded", String(opening));
    });
  }

  /* ------------------------------------------------------------------
     Schedule: if any tagged session name would wrap next to its tag,
     the whole day switches to the stacked layout.
  ------------------------------------------------------------------ */

  for (const day of document.querySelectorAll("[data-schedule]")) {
    const measure = day.parentElement.querySelector("[data-schedule-measure]");
    if (!measure) continue;

    const check = () => {
      const wraps = [...measure.querySelectorAll("[data-measure-name]")].some((name) => {
        const lineHeight = parseFloat(getComputedStyle(name).lineHeight);
        return name.scrollHeight > lineHeight * 1.4;
      });
      day.toggleAttribute("data-stacked", wraps);
    };

    check();
    new ResizeObserver(check).observe(measure);
  }

  /* ------------------------------------------------------------------
     Team marquee: the browser only loads a lazy image once it's on screen,
     and most of the strip is off to the side, so headshots would load one
     by one as they slide in, each showing a blank card first. Instead, load
     the whole strip when the section is about a screen away.
  ------------------------------------------------------------------ */

  for (const strip of document.querySelectorAll("[data-marquee]")) {
    new IntersectionObserver((entries, observer) => {
      if (!entries.some((entry) => entry.isIntersecting)) return;
      for (const img of strip.querySelectorAll('img[loading="lazy"]')) img.loading = "eager";
      observer.disconnect();
    }, { rootMargin: "100% 0px" }).observe(strip);
  }

  /* ------------------------------------------------------------------
     On load: jump to the card in the URL (#faq), or open a popup (#apply).
  ------------------------------------------------------------------ */

  const startId = location.hash.slice(1);
  if (startId && document.getElementById(startId)) {
    requestAnimationFrame(() => scrollToCard(startId, "instant"));
  }
  openFromHash();
})();
