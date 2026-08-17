import Foundation
import Combine
import StoreKit

/// StoreKit 订阅购买服务。
@MainActor
final class StoreKitPurchaseService: ObservableObject {
    /// 已加载的商品。
    @Published private(set) var productsByPlan: [SubscriptionPlan: StoreProduct] = [:]

    /// 当前用户是否拥有 Plus 权益。
    @Published private(set) var isSubscribed: Bool = false

    /// 当前购买状态说明。
    @Published private(set) var statusMessage: String?

    /// 原始 StoreKit 商品。
    private var productsById: [String: Product] = [:]

    /// 交易更新监听任务。
    private var transactionUpdatesTask: Task<Void, Never>?

    /// 创建 StoreKit 购买服务。
    init() {
        transactionUpdatesTask = listenForTransactionUpdates()
        Task {
            await refreshEntitlements()
        }
    }

    /// 释放交易监听。
    deinit {
        transactionUpdatesTask?.cancel()
    }

    /// 加载 App Store 订阅商品。
    func loadProducts() async {
        do {
            let productIds = SubscriptionPlan.allCases.map(\.productId)
            let products = try await Product.products(for: productIds)
            productsById = Dictionary(uniqueKeysWithValues: products.map { ($0.id, $0) })
            productsByPlan = Dictionary(uniqueKeysWithValues: products.compactMap { product in
                guard let plan = SubscriptionPlan.allCases.first(where: { $0.productId == product.id }) else {
                    return nil
                }
                return (plan, StoreProduct(
                    id: product.id,
                    plan: plan,
                    displayName: product.displayName,
                    displayPrice: product.displayPrice,
                    description: product.description
                ))
            })
            if productsByPlan.isEmpty {
                statusMessage = "StoreKit products are not configured yet."
            } else {
                statusMessage = nil
            }
            await refreshEntitlements()
        } catch {
            statusMessage = error.localizedDescription
        }
    }

    /// 购买指定订阅套餐。
    ///
    /// - Parameter plan: 订阅套餐
    /// - Returns: 是否完成购买
    func purchase(plan: SubscriptionPlan) async throws -> Bool {
        guard let product = productsById[plan.productId] else {
            throw StoreKitPurchaseError.productUnavailable
        }
        let result = try await product.purchase()
        switch result {
        case .success(let verification):
            let transaction = try verified(verification)
            await transaction.finish()
            await refreshEntitlements()
            return true
        case .userCancelled:
            return false
        case .pending:
            statusMessage = "Purchase is pending approval."
            return false
        @unknown default:
            throw StoreKitPurchaseError.unknownPurchaseResult
        }
    }

    /// 恢复购买并刷新权益。
    func restorePurchases() async {
        do {
            try await AppStore.sync()
            await refreshEntitlements()
            statusMessage = isSubscribed ? "Subscription restored." : "No active subscription was found."
        } catch {
            statusMessage = error.localizedDescription
        }
    }

    /// 刷新当前权益。
    func refreshEntitlements() async {
        let productIds = Set(SubscriptionPlan.allCases.map(\.productId))
        var hasActiveSubscription = false
        for await entitlement in Transaction.currentEntitlements {
            guard let transaction = try? verified(entitlement),
                  productIds.contains(transaction.productID),
                  transaction.revocationDate == nil else {
                continue
            }
            if let expirationDate = transaction.expirationDate {
                hasActiveSubscription = expirationDate > Date()
            } else {
                hasActiveSubscription = true
            }
            if hasActiveSubscription {
                break
            }
        }
        isSubscribed = hasActiveSubscription
    }

    /// 监听 StoreKit 交易更新。
    ///
    /// - Returns: 监听任务
    private func listenForTransactionUpdates() -> Task<Void, Never> {
        Task {
            for await update in Transaction.updates {
                guard let transaction = try? verified(update) else {
                    continue
                }
                await transaction.finish()
                await refreshEntitlements()
            }
        }
    }

    /// 校验 StoreKit 签名结果。
    ///
    /// - Parameter result: StoreKit 校验结果
    /// - Returns: 已校验的值
    private func verified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .verified(let value):
            return value
        case .unverified:
            throw StoreKitPurchaseError.unverifiedTransaction
        }
    }
}

/// StoreKit 购买错误。
enum StoreKitPurchaseError: LocalizedError {
    /// 商品不可用。
    case productUnavailable
    /// 交易未通过校验。
    case unverifiedTransaction
    /// 未知购买结果。
    case unknownPurchaseResult

    /// 错误说明。
    var errorDescription: String? {
        switch self {
        case .productUnavailable:
            return "This subscription product is not configured in StoreKit yet."
        case .unverifiedTransaction:
            return "The App Store transaction could not be verified."
        case .unknownPurchaseResult:
            return "The purchase returned an unknown result."
        }
    }
}
